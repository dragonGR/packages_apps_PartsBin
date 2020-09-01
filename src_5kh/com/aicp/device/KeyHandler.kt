/*
* Copyright (C) 2016 The OmniROM Project
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*/
package com.aicp.device

import PackageManager.NameNotFoundException
import android.app.ActivityManagerNative
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.IAudioService
import android.media.session.MediaSessionLegacyHelper
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.os.RemoteException
import android.os.ServiceManager
import android.os.SystemClock
import android.os.SystemProperties
import android.os.UserHandle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.provider.Settings.Global
import android.provider.Settings.Global.ZEN_MODE_ALARMS
import android.provider.Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS
import android.provider.Settings.Global.ZEN_MODE_NO_INTERRUPTIONS
import android.provider.Settings.Global.ZEN_MODE_OFF
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.text.TextUtils
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.WindowManagerGlobal
import com.aicp.device.AppSelectListPreference
import com.android.internal.statusbar.IStatusBarService
import com.android.internal.util.ArrayUtils
import com.android.internal.util.aicp.AicpUtils
import com.android.internal.util.aicp.AicpVibe
import com.android.internal.util.aicp.CustomKeyHandler

class KeyHandler(context: Context) : CustomKeyHandler {
    protected val mContext: Context
    private val mPowerManager: PowerManager
    private val mEventHandler: EventHandler
    private val mGestureWakeLock: WakeLock
    private val mHandler: Handler = Handler()
    private val mSettingsObserver: SettingsObserver
    private val mNoMan: NotificationManager
    private val mAudioManager: AudioManager
    private val mSensorManager: SensorManager
    private var mProxyIsNear = false
    private var mUseProxiCheck = false
    private val mTiltSensor: Sensor?
    private var mUseTiltCheck = false
    private var mProxyWasNear = false
    private var mProxySensorTimestamp: Long = 0
    private var mUseWaveCheck = false
    private val mPocketSensor: Sensor?
    private var mUsePocketCheck = false
    private var mFPcheck = false
    private var mDispOn: Boolean
    private var isFpgesture = false
    private var mTorchState = false
    private var mUseSliderTorch = false
    private var mSliderPosition = -1
    private val mProximitySensor: SensorEventListener = object : SensorEventListener() {
        fun onSensorChanged(event: SensorEvent) {
            mProxyIsNear = event.values.get(0) === 1
            if (DEBUG_SENSOR) Log.i(TAG, "mProxyIsNear = $mProxyIsNear mProxyWasNear = $mProxyWasNear")
            if (mUseProxiCheck) {
                if (!sIsOnePlus5t) {
                    if (Utils.fileWritable(FPC_CONTROL_PATH)) {
                        Utils.writeValue(FPC_CONTROL_PATH, if (mProxyIsNear) "1" else "0")
                    }
                } else {
                    if (Utils.fileWritable(GOODIX_CONTROL_PATH)) {
                        Utils.writeValue(GOODIX_CONTROL_PATH, if (mProxyIsNear) "1" else "0")
                    }
                }
            }
            if (mUseWaveCheck || mUsePocketCheck) {
                if (mProxyWasNear && !mProxyIsNear) {
                    val delta: Long = SystemClock.elapsedRealtime() - mProxySensorTimestamp
                    if (DEBUG_SENSOR) Log.i(TAG, "delta = $delta")
                    if (mUseWaveCheck && delta < Constants.HANDWAVE_MAX_DELTA_MS) {
                        launchDozePulse()
                    }
                    if (mUsePocketCheck && delta > Constants.POCKET_MIN_DELTA_MS) {
                        launchDozePulse()
                    }
                }
                mProxySensorTimestamp = SystemClock.elapsedRealtime()
                mProxyWasNear = mProxyIsNear
            }
        }

        fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
    private val mTiltSensorListener: SensorEventListener = object : SensorEventListener() {
        fun onSensorChanged(event: SensorEvent) {
            if (event.values.get(0) === 1) {
                launchDozePulse()
            }
        }

        fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private inner class SettingsObserver internal constructor(handler: Handler?) : ContentObserver(handler) {
        fun observe() {
            mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(
                    Settings.Secure.HARDWARE_KEYS_DISABLE),
                    false, this)
            mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(
                    Settings.System.OMNI_DEVICE_PROXI_CHECK_ENABLED),
                    false, this)
            mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(
                    Settings.System.OMNI_DEVICE_FEATURE_SETTINGS),
                    false, this)
            update()
            updateDozeSettings()
        }

        fun onChange(selfChange: Boolean) {
            update()
        }

        fun onChange(selfChange: Boolean, uri: Uri) {
            if (uri.equals(Settings.System.getUriFor(
                            Settings.System.OMNI_DEVICE_FEATURE_SETTINGS))) {
                updateDozeSettings()
                return
            }
            update()
        }

        fun update() {
            setButtonDisable(mContext)
            mUseProxiCheck = Settings.System.getIntForUser(
                    mContext.getContentResolver(), Settings.System.OMNI_DEVICE_PROXI_CHECK_ENABLED, 1,
                    UserHandle.USER_CURRENT) === 1
        }
    }

    private val mScreenStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        fun onReceive(context: Context?, intent: Intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                mDispOn = true
                onDisplayOn()
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                mDispOn = false
                onDisplayOff()
            }
        }
    }

    private inner class EventHandler : Handler() {
        fun handleMessage(msg: Message?) {}
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.getAction() !== KeyEvent.ACTION_UP) {
            return false
        }
        isFpgesture = false
        val isKeySupported: Boolean = ArrayUtils.contains(Constants.sHandledGestures, event.getScanCode())
        if (isKeySupported) {
            val scanCode: Int = event.getScanCode()
            if (DEBUG) Log.i(TAG, "scanCode=$scanCode")
            val position = if (scanCode == Constants.KEY_SLIDER_TOP) 0 else if (scanCode == Constants.KEY_SLIDER_CENTER) 1 else 2
            if (mSliderPosition != position) {
                mSliderPosition = position
                doHandleSliderAction(position)
                when (scanCode) {
                    Constants.KEY_SLIDER_TOP -> {
                        if (DEBUG) Log.i(TAG, "KEY_SLIDER_TOP")
                        return true
                    }
                    Constants.KEY_SLIDER_CENTER -> {
                        if (DEBUG) Log.i(TAG, "KEY_SLIDER_CENTER")
                        return true
                    }
                    Constants.KEY_SLIDER_BOTTOM -> {
                        if (DEBUG) Log.i(TAG, "KEY_SLIDER_BOTTOM")
                        return true
                    }
                }
            } // else: discard changes caused by a loose contact
        }
        if (DEBUG) Log.i(TAG, "nav_code=" + event.getScanCode())
        val fpcode: Int = event.getScanCode()
        mFPcheck = canHandleKeyEvent(event)
        val value = getGestureValueForFPScanCode(fpcode)
        if (mFPcheck && mDispOn && !TextUtils.isEmpty(value) && value != AppSelectListPreference.Companion.DISABLED_ENTRY) {
            isFpgesture = true
            if (!launchSpecialActions(value) && !isCameraLaunchEvent(event)) {
                AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false,
                        mContext, DeviceSettings.Companion.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME, Constants.GESTURE_HAPTIC_DURATION)
                val intent: Intent = createIntent(value)
                if (DEBUG) Log.i(TAG, "intent = $intent")
                mContext.startActivity(intent)
            }
        }
        return isKeySupported
    }

    fun canHandleKeyEvent(event: KeyEvent): Boolean {
        return if (sIsOnePlus5t) {
            ArrayUtils.contains(Constants.sSupportedGestures5t, event.getScanCode())
        } else {
            ArrayUtils.contains(Constants.sSupportedGestures, event.getScanCode())
        }
    }

    fun isDisabledKeyEvent(event: KeyEvent): Boolean {
        val isProxyCheckRequired = mUseProxiCheck &&
                ArrayUtils.contains(Constants.sProxiCheckedGestures, event.getScanCode())
        if (mProxyIsNear && isProxyCheckRequired) {
            if (DEBUG) Log.i(TAG, "isDisabledKeyEvent: blocked by proxi sensor - scanCode=" + event.getScanCode())
            return true
        }
        return false
    }

    fun isCameraLaunchEvent(event: KeyEvent): Boolean {
        if (event.getAction() !== KeyEvent.ACTION_UP) {
            return false
        }
        return if (mFPcheck) {
            val value = getGestureValueForFPScanCode(event.getScanCode())
            !TextUtils.isEmpty(value) && value == AppSelectListPreference.Companion.CAMERA_ENTRY
        } else {
            val value = getGestureValueForScanCode(event.getScanCode())
            !TextUtils.isEmpty(value) && value == AppSelectListPreference.Companion.CAMERA_ENTRY
        }
    }

    fun isWakeEvent(event: KeyEvent): Boolean {
        if (event.getAction() !== KeyEvent.ACTION_UP) {
            return false
        }
        val value = getGestureValueForScanCode(event.getScanCode())
        if (!TextUtils.isEmpty(value) && value == AppSelectListPreference.Companion.WAKE_ENTRY) {
            if (DEBUG) Log.i(TAG, "isWakeEvent " + event.getScanCode().toString() + value)
            return true
        }
        return event.getScanCode() === Constants.KEY_DOUBLE_TAP
    }

    fun isActivityLaunchEvent(event: KeyEvent): Intent? {
        if (event.getAction() !== KeyEvent.ACTION_UP) {
            return null
        }
        val value = getGestureValueForScanCode(event.getScanCode())
        if (!TextUtils.isEmpty(value) && value != AppSelectListPreference.Companion.DISABLED_ENTRY) {
            if (DEBUG) Log.i(TAG, "isActivityLaunchEvent " + event.getScanCode().toString() + value)
            if (!launchSpecialActions(value)) {
                AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false,
                        mContext, DeviceSettings.Companion.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME, Constants.GESTURE_HAPTIC_DURATION)
                return createIntent(value)
            }
        }
        return null
    }

    private val audioService: IAudioService?
        private get() {
            val audioService: IAudioService = IAudioService.Stub
                    .asInterface(ServiceManager.checkService(Context.AUDIO_SERVICE))
            if (audioService == null) {
                Log.w(TAG, "Unable to find IAudioService interface.")
            }
            return audioService
        }

    val isMusicActive: Boolean
        get() = mAudioManager.isMusicActive()

    private fun dispatchMediaKeyWithWakeLockToAudioService(keycode: Int) {
        if (ActivityManagerNative.isSystemReady()) {
            val audioService: IAudioService? = audioService
            if (audioService != null) {
                var event = KeyEvent(SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN,
                        keycode, 0)
                dispatchMediaKeyEventUnderWakelock(event)
                event = KeyEvent.changeAction(event, KeyEvent.ACTION_UP)
                dispatchMediaKeyEventUnderWakelock(event)
            }
        }
    }

    private fun dispatchMediaKeyEventUnderWakelock(event: KeyEvent) {
        if (ActivityManagerNative.isSystemReady()) {
            MediaSessionLegacyHelper.getHelper(mContext).sendMediaButtonEvent(event, true)
        }
    }

    private fun onDisplayOn() {
        if (DEBUG) Log.i(TAG, "Display on")
        if (enableProxiSensor()) {
            mSensorManager.unregisterListener(mProximitySensor, mPocketSensor)
            enableGoodix()
        }
        if (mUseTiltCheck) {
            mSensorManager.unregisterListener(mTiltSensorListener, mTiltSensor)
        }
    }

    private fun enableGoodix() {
        if (sIsOnePlus5t) {
            if (Utils.fileWritable(GOODIX_CONTROL_PATH)) {
                Utils.writeValue(GOODIX_CONTROL_PATH, "0")
            }
        }
    }

    private fun onDisplayOff() {
        if (DEBUG) Log.i(TAG, "Display off")
        if (enableProxiSensor()) {
            mProxyWasNear = false
            mSensorManager.registerListener(mProximitySensor, mPocketSensor,
                    SensorManager.SENSOR_DELAY_NORMAL)
            mProxySensorTimestamp = SystemClock.elapsedRealtime()
        }
        if (mUseTiltCheck) {
            mSensorManager.registerListener(mTiltSensorListener, mTiltSensor,
                    SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun getSliderAction(position: Int): Int {
        var value: String? = Settings.System.getStringForUser(mContext.getContentResolver(),
                Settings.System.OMNI_BUTTON_EXTRA_KEY_MAPPING,
                UserHandle.USER_CURRENT)
        val defaultValue: String = DeviceSettings.Companion.SLIDER_DEFAULT_VALUE
        if (value == null) {
            value = defaultValue
        } else if (value.indexOf(",") == -1) {
            value = defaultValue
        }
        try {
            val parts = value.split(",".toRegex()).toTypedArray()
            return Integer.valueOf(parts[position])
        } catch (e: Exception) {
        }
        return 0
    }

    private fun doHandleSliderAction(position: Int) {
        val action = getSliderAction(position)
        var positionValue = 0
        if (action == 0) {
            mNoMan.setZenMode(ZEN_MODE_OFF, null, TAG)
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL)
            mTorchState = false
            positionValue = Constants.MODE_RING
        } else if (action == 1) {
            mNoMan.setZenMode(ZEN_MODE_OFF, null, TAG)
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_VIBRATE)
            mTorchState = false
            positionValue = Constants.MODE_VIBRATE
        } else if (action == 2) {
            mNoMan.setZenMode(ZEN_MODE_OFF, null, TAG)
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_SILENT)
            mTorchState = false
            positionValue = Constants.MODE_SILENT
        } else if (action == 3) {
            mNoMan.setZenMode(ZEN_MODE_IMPORTANT_INTERRUPTIONS, null, TAG)
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL)
            mTorchState = false
            positionValue = Constants.MODE_PRIORITY_ONLY
        } else if (action == 4) {
            mNoMan.setZenMode(ZEN_MODE_ALARMS, null, TAG)
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL)
            mTorchState = false
            positionValue = Constants.MODE_ALARMS_ONLY
        } else if (action == 5) {
            mNoMan.setZenMode(ZEN_MODE_NO_INTERRUPTIONS, null, TAG)
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL)
            mTorchState = false
            positionValue = Constants.MODE_TOTAL_SILENCE
        } else if (action == 6) {
            mNoMan.setZenMode(ZEN_MODE_OFF, null, TAG)
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL)
            positionValue = Constants.MODE_FLASHLIGHT
            mUseSliderTorch = true
            mTorchState = true
        }
        if (positionValue != 0) {
            sendUpdateBroadcast(position, positionValue)
        }
        if ((!mProxyIsNear && mUseProxiCheck || !mUseProxiCheck) && mUseSliderTorch && action < 4) {
            launchSpecialActions(AppSelectListPreference.Companion.TORCH_ENTRY)
            mUseSliderTorch = false
        } else if ((!mProxyIsNear && mUseProxiCheck || !mUseProxiCheck) && mUseSliderTorch) {
            launchSpecialActions(AppSelectListPreference.Companion.TORCH_ENTRY)
        }
    }

    private fun sendUpdateBroadcast(position: Int, position_value: Int) {
        val extras = Bundle()
        val intent = Intent(Constants.ACTION_UPDATE_SLIDER_POSITION)
        extras.putInt(Constants.EXTRA_SLIDER_POSITION, position)
        extras.putInt(Constants.EXTRA_SLIDER_POSITION_VALUE, position_value)
        intent.putExtras(extras)
        mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT)
        intent.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY)
        Log.d(TAG, "slider change to positon " + position
                + " with value " + position_value)
    }

    private fun createIntent(value: String?): Intent {
        val componentName: ComponentName = ComponentName.unflattenFromString(value)
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        intent.setComponent(componentName)
        return intent
    }

    private fun launchSpecialActions(value: String?): Boolean {
        val musicPlaybackEnabled = Settings.System.getIntForUser(mContext.getContentResolver(),
                "Settings.System." + DeviceSettings.Companion.GESTURE_MUSIC_PLAYBACK_SETTINGS_VARIABLE_NAME, 0, UserHandle.USER_CURRENT) === 1
        /* handle music playback gesture if enabled */if (musicPlaybackEnabled) {
            when (value) {
                AppSelectListPreference.Companion.MUSIC_PLAY_ENTRY -> {
                    mGestureWakeLock.acquire(Constants.GESTURE_WAKELOCK_DURATION)
                    AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false, mContext, DeviceSettings.Companion.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME, Constants.GESTURE_HAPTIC_DURATION)
                    dispatchMediaKeyWithWakeLockToAudioService(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                    return true
                }
                AppSelectListPreference.Companion.MUSIC_NEXT_ENTRY -> {
                    if (isMusicActive) {
                        mGestureWakeLock.acquire(Constants.GESTURE_WAKELOCK_DURATION)
                        AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false, mContext, DeviceSettings.Companion.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME, Constants.GESTURE_HAPTIC_DURATION)
                        dispatchMediaKeyWithWakeLockToAudioService(KeyEvent.KEYCODE_MEDIA_NEXT)
                    }
                    return true
                }
                AppSelectListPreference.Companion.MUSIC_PREV_ENTRY -> {
                    if (isMusicActive) {
                        mGestureWakeLock.acquire(Constants.GESTURE_WAKELOCK_DURATION)
                        AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false, mContext, DeviceSettings.Companion.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME, Constants.GESTURE_HAPTIC_DURATION)
                        dispatchMediaKeyWithWakeLockToAudioService(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                    }
                    return true
                }
            }
        }
        if (value == AppSelectListPreference.Companion.TORCH_ENTRY) {
            mGestureWakeLock.acquire(Constants.GESTURE_WAKELOCK_DURATION)
            val service: IStatusBarService = statusBarService
            if (service != null) {
                try {
                    return if (mUseSliderTorch) {
                        service.toggleCameraFlashState(mTorchState)
                        AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false,
                                mContext, DeviceSettings.Companion.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                                Constants.GESTURE_HAPTIC_DURATION)
                        true
                    } else {
                        service.toggleCameraFlash()
                        AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false,
                                mContext, DeviceSettings.Companion.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                                Constants.GESTURE_HAPTIC_DURATION)
                        true
                    }
                } catch (e: RemoteException) {
                    // do nothing.
                }
            }
        } else if (value == AppSelectListPreference.Companion.VOLUME_UP_ENTRY) {
            AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false,
                    mContext, DeviceSettings.Companion.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                    Constants.GESTURE_HAPTIC_DURATION)
            mAudioManager.adjustSuggestedStreamVolume(AudioManager.ADJUST_RAISE, AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI)
            return true
        } else if (value == AppSelectListPreference.Companion.VOLUME_DOWN_ENTRY) {
            AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false,
                    mContext, DeviceSettings.Companion.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                    Constants.GESTURE_HAPTIC_DURATION)
            mAudioManager.adjustSuggestedStreamVolume(AudioManager.ADJUST_LOWER, AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI)
            return true
        } else if (value == AppSelectListPreference.Companion.BROWSE_SCROLL_DOWN_ENTRY) {
            AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false,
                    mContext, DeviceSettings.Companion.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                    Constants.GESTURE_HAPTIC_DURATION)
            AicpUtils.sendKeycode(KeyEvent.KEYCODE_PAGE_DOWN)
            return true
        } else if (value == AppSelectListPreference.Companion.BROWSE_SCROLL_UP_ENTRY) {
            AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false,
                    mContext, DeviceSettings.Companion.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                    Constants.GESTURE_HAPTIC_DURATION)
            AicpUtils.sendKeycode(KeyEvent.KEYCODE_PAGE_UP)
            return true
        } else if (value == AppSelectListPreference.Companion.NAVIGATE_BACK_ENTRY) {
            AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false,
                    mContext, DeviceSettings.Companion.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                    Constants.GESTURE_HAPTIC_DURATION)
            AicpUtils.sendKeycode(KeyEvent.KEYCODE_BACK)
            return true
        } else if (value == AppSelectListPreference.Companion.NAVIGATE_HOME_ENTRY) {
            AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false,
                    mContext, DeviceSettings.Companion.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                    Constants.GESTURE_HAPTIC_DURATION)
            AicpUtils.sendKeycode(KeyEvent.KEYCODE_HOME)
            return true
        } else if (value == AppSelectListPreference.Companion.NAVIGATE_RECENT_ENTRY) {
            AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false,
                    mContext, DeviceSettings.Companion.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                    Constants.GESTURE_HAPTIC_DURATION)
            AicpUtils.sendKeycode(KeyEvent.KEYCODE_APP_SWITCH)
            return true
        }
        return false
    }

    private fun getGestureValueForScanCode(scanCode: Int): String? {
        /* for the music playback gestures, just return the expected values */
        when (scanCode) {
            Constants.GESTURE_II_SCANCODE -> return AppSelectListPreference.Companion.MUSIC_PLAY_ENTRY
            Constants.GESTURE_CIRCLE_SCANCODE -> return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.Companion.DEVICE_GESTURE_MAPPING_1, UserHandle.USER_CURRENT)
            Constants.GESTURE_V_SCANCODE -> return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.Companion.DEVICE_GESTURE_MAPPING_2, UserHandle.USER_CURRENT)
            Constants.GESTURE_M_SCANCODE -> return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.Companion.DEVICE_GESTURE_MAPPING_3, UserHandle.USER_CURRENT)
            Constants.GESTURE_LEFT_V_SCANCODE -> return AppSelectListPreference.Companion.MUSIC_PREV_ENTRY
            Constants.GESTURE_RIGHT_V_SCANCODE -> return AppSelectListPreference.Companion.MUSIC_NEXT_ENTRY
            Constants.GESTURE_DOWN_SWIPE_SCANCODE -> return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.Companion.DEVICE_GESTURE_MAPPING_6, UserHandle.USER_CURRENT)
            Constants.GESTURE_UP_SWIPE_SCANCODE -> return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.Companion.DEVICE_GESTURE_MAPPING_7, UserHandle.USER_CURRENT)
            Constants.GESTURE_LEFT_SWIPE_SCANCODE -> return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.Companion.DEVICE_GESTURE_MAPPING_8, UserHandle.USER_CURRENT)
            Constants.GESTURE_RIGHT_SWIPE_SCANCODE -> return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.Companion.DEVICE_GESTURE_MAPPING_9, UserHandle.USER_CURRENT)
            Constants.GESTURE_S_SCANCODE -> return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.Companion.DEVICE_GESTURE_MAPPING_10, UserHandle.USER_CURRENT)
            Constants.GESTURE_W_SCANCODE -> return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.Companion.DEVICE_GESTURE_MAPPING_11, UserHandle.USER_CURRENT)
        }
        return null
    }

    private fun getGestureValueForFPScanCode(scanCode: Int): String? {
        when (scanCode) {
            Constants.FP_GESTURE_SWIPE_DOWN -> if (areSystemNavigationKeysEnabled() == false) {
                return Settings.System.getStringForUser(mContext.getContentResolver(),
                        GestureSettings.Companion.DEVICE_GESTURE_MAPPING_10, UserHandle.USER_CURRENT)
            }
            Constants.FP_GESTURE_SWIPE_UP -> if (areSystemNavigationKeysEnabled() == false) {
                return Settings.System.getStringForUser(mContext.getContentResolver(),
                        GestureSettings.Companion.DEVICE_GESTURE_MAPPING_11, UserHandle.USER_CURRENT)
            }
            Constants.FP_GESTURE_SWIPE_LEFT -> return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.Companion.DEVICE_GESTURE_MAPPING_12, UserHandle.USER_CURRENT)
            Constants.FP_GESTURE_SWIPE_RIGHT -> return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.Companion.DEVICE_GESTURE_MAPPING_13, UserHandle.USER_CURRENT)
            Constants.FP_GESTURE_LONG_PRESS -> return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.Companion.DEVICE_GESTURE_MAPPING_14, UserHandle.USER_CURRENT)
        }
        return null
    }

    private fun areSystemNavigationKeysEnabled(): Boolean {
        return Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.SYSTEM_NAVIGATION_KEYS_ENABLED, 0, UserHandle.USER_CURRENT) === 1
    }

    private fun launchDozePulse() {
        if (DEBUG) Log.i(TAG, "Doze pulse")
        mContext.sendBroadcastAsUser(Intent(Constants.DOZE_INTENT),
                UserHandle(UserHandle.USER_CURRENT))
    }

    private fun enableProxiSensor(): Boolean {
        return mUsePocketCheck || mUseWaveCheck || mUseProxiCheck
    }

    private fun updateDozeSettings() {
        val value: String = Settings.System.getStringForUser(mContext.getContentResolver(),
                Settings.System.OMNI_DEVICE_FEATURE_SETTINGS,
                UserHandle.USER_CURRENT)
        if (DEBUG) Log.i(TAG, "Doze settings = $value")
        if (!TextUtils.isEmpty(value)) {
            val parts = value.split(":".toRegex()).toTypedArray()
            mUseWaveCheck = java.lang.Boolean.valueOf(parts[0])
            mUsePocketCheck = java.lang.Boolean.valueOf(parts[1])
            mUseTiltCheck = java.lang.Boolean.valueOf(parts[2])
        }
    }

    val statusBarService: IStatusBarService
        get() = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"))

    fun getCustomProxiIsNear(event: SensorEvent): Boolean {
        return event.values.get(0) === 1
    }

    /*private void vibe(){
        / *boolean doVibrate = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.OMNI_DEVICE_GESTURE_FEEDBACK_ENABLED, 0,
                UserHandle.USER_CURRENT) == 1;
        int owningUid;
        String owningPackage;

        owningUid = android.os.Process.myUid();
        owningPackage = mContext.getOpPackageName();
        VibrationEffect effect = VibrationEffect.get(VibrationEffect.EFFECT_HEAVY_CLICK);
        //mVibrator.vibrate(owningUid, owningPackage, effect, VIBRATION_ATTRIBUTES);
        //OmniVibe.performHapticFeedback(owningUid, owningPackage, effect, VIBRATION_ATTRIBUTES);

        //OmniVibe mOmniVibe = new OmniVibe();
        OmniVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false, mContext);
    }*/
    val customProxiSensor: String
        get() = "com.oneplus.sensor.pocket"

    companion object {
        private const val TAG = "KeyHandler"
        private const val DEBUG = false
        private const val DEBUG_SENSOR = false
        private const val KEY_CONTROL_PATH = "/proc/touchpanel/key_disable"
        private const val FPC_CONTROL_PATH = "/sys/devices/soc/soc:fpc_fpc1020/proximity_state"
        private const val FPC_KEY_CONTROL_PATH = "/sys/devices/soc/soc:fpc_fpc1020/key_disable"
        private const val GOODIX_CONTROL_PATH = "/sys/devices/soc/soc:goodix_fp/proximity_state"
        private val sIsOnePlus5t: Boolean = android.os.Build.DEVICE.equals("dumpling")
        private var mButtonDisabled = false
        fun setButtonDisable(context: Context) {
            // we should never come here on the 5t but just to be sure
            if (!sIsOnePlus5t) {
                mButtonDisabled = Settings.Secure.getIntForUser(
                        context.getContentResolver(), Settings.Secure.HARDWARE_KEYS_DISABLE, 0,
                        UserHandle.USER_CURRENT) === 1
                if (DEBUG) Log.i(TAG, "setButtonDisable=$mButtonDisabled")
                if (mButtonDisabled) {
                    Utils.writeValue(KEY_CONTROL_PATH, "1")
                    Utils.writeValue(FPC_KEY_CONTROL_PATH, "1")
                } else {
                    Utils.writeValue(KEY_CONTROL_PATH, "0")
                    Utils.writeValue(FPC_KEY_CONTROL_PATH, "0")
                }
            }
        }

        protected fun getSensor(sm: SensorManager, type: String): Sensor? {
            for (sensor in sm.getSensorList(Sensor.TYPE_ALL)) {
                if (type == sensor.getStringType()) {
                    return sensor
                }
            }
            return null
        }
    }

    init {
        mContext = context
        mDispOn = true
        mEventHandler = EventHandler()
        mPowerManager = mContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        mGestureWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "GestureWakeLock")
        mSettingsObserver = SettingsObserver(mHandler)
        mSettingsObserver.observe()
        mNoMan = mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mAudioManager = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mSensorManager = mContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mTiltSensor = getSensor(mSensorManager, "com.oneplus.sensor.pickup")
        mPocketSensor = getSensor(mSensorManager, "com.oneplus.sensor.pocket")
        val screenStateFilter = IntentFilter(Intent.ACTION_SCREEN_ON)
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF)
        mContext.registerReceiver(mScreenStateReceiver, screenStateFilter)
    }
}