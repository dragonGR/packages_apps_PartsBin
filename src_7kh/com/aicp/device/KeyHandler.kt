/*
 * Copyright (C) 2015-2016 The CyanogenMod Project
 * Copyright (C) 2017 The LineageOS Project
 * Copyright (C) 2020 Android Ice Cold Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import android.media.AudioManager
import android.media.IAudioService
import android.media.session.MediaSessionLegacyHelper
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.os.RemoteException
import android.os.ServiceManager
import android.os.SystemClock
import android.os.UserHandle
import android.provider.Settings
import android.provider.Settings.Global
import android.provider.Settings.Global.ZEN_MODE_ALARMS
import android.provider.Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS
import android.provider.Settings.Global.ZEN_MODE_NO_INTERRUPTIONS
import android.provider.Settings.Global.ZEN_MODE_OFF
import android.text.TextUtils
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import com.aicp.device.AppSelectListPreference
import com.aicp.device.KeyHandler.SettingsObserver
import com.android.internal.statusbar.IStatusBarService
import com.android.internal.util.ArrayUtils
import com.android.internal.util.aicp.AicpVibe
import com.android.internal.util.aicp.CustomKeyHandler

class KeyHandler(context: Context) : CustomKeyHandler {
    protected val mContext: Context
    private val mPowerManager: PowerManager
    private val mEventHandler: EventHandler
    private val mGestureWakeLock: WakeLock
    private val mHandler: Handler = Handler()
    private val mNotificationManager: NotificationManager
    private val mAudioManager: AudioManager
    private var mDispOn: Boolean
    private var mTorchState = false
    private var mUseSliderTorch = false
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

    private fun hasSetupCompleted(): Boolean {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.USER_SETUP_COMPLETE, 0) !== 0
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.getAction() !== KeyEvent.ACTION_UP) {
            return false
        }
        if (!hasSetupCompleted()) {
            return false
        }
        val isKeySupported: Boolean = ArrayUtils.contains(Constants.sHandledGestures, event.getScanCode())
        if (isKeySupported) {
            val scanCode: Int = event.getScanCode()
            if (DEBUG) Log.i(TAG, "scanCode=$scanCode")
            val position = if (scanCode == Constants.KEY_SLIDER_TOP) 2 else if (scanCode == Constants.KEY_SLIDER_CENTER) 1 else 0
            if (scanCode == KEY_SINGLE_TAP) {
                launchDozePulse()
                return true
            }
            doHandleSliderAction(position)
        }
        return isKeySupported
    }

    fun canHandleKeyEvent(event: KeyEvent): Boolean {
        return ArrayUtils.contains(Constants.sSupportedGestures, event.getScanCode())
    }

    fun isDisabledKeyEvent(event: KeyEvent?): Boolean {
        return false
    }

    fun isCameraLaunchEvent(event: KeyEvent): Boolean {
        if (event.getAction() !== KeyEvent.ACTION_UP) {
            return false
        }
        val value = getGestureValueForScanCode(event.getScanCode())
        return !TextUtils.isEmpty(value) && value == AppSelectListPreference.Companion.CAMERA_ENTRY
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
                AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false, mContext,
                        DeviceSettings.Companion.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME, Constants.GESTURE_HAPTIC_DURATION)
                return createIntent(value)
            }
        }
        return null
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
            Constants.GESTURE_S_SCANCODE -> return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.Companion.DEVICE_GESTURE_MAPPING_10, UserHandle.USER_CURRENT)
            Constants.GESTURE_W_SCANCODE -> return Settings.System.getStringForUser(mContext.getContentResolver(),
                    GestureSettings.Companion.DEVICE_GESTURE_MAPPING_11, UserHandle.USER_CURRENT)
        }
        return null
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
            mNotificationManager.setZenMode(ZEN_MODE_OFF, null, TAG)
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL)
            mTorchState = false
            positionValue = Constants.MODE_RING
        } else if (action == 1) {
            mNotificationManager.setZenMode(ZEN_MODE_OFF, null, TAG)
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_VIBRATE)
            mTorchState = false
            positionValue = Constants.MODE_VIBRATE
        } else if (action == 2) {
            mNotificationManager.setZenMode(ZEN_MODE_OFF, null, TAG)
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_SILENT)
            mTorchState = false
            positionValue = Constants.MODE_SILENT
        } else if (action == 3) {
            mNotificationManager.setZenMode(ZEN_MODE_IMPORTANT_INTERRUPTIONS, null, TAG)
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL)
            mTorchState = false
            positionValue = Constants.MODE_PRIORITY_ONLY
        } else if (action == 4) {
            mNotificationManager.setZenMode(ZEN_MODE_ALARMS, null, TAG)
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL)
            mTorchState = false
            positionValue = Constants.MODE_ALARMS_ONLY
        } else if (action == 5) {
            mNotificationManager.setZenMode(ZEN_MODE_NO_INTERRUPTIONS, null, TAG)
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL)
            mTorchState = false
            positionValue = Constants.MODE_TOTAL_SILENCE
        } else if (action == 6) {
            mNotificationManager.setZenMode(ZEN_MODE_OFF, null, TAG)
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL)
            positionValue = Constants.MODE_FLASHLIGHT
            mUseSliderTorch = true
            mTorchState = true
        }
        if (positionValue != 0) {
            sendUpdateBroadcast(position, positionValue)
        }
        if (mUseSliderTorch && action < 6) {
            launchSpecialActions(AppSelectListPreference.Companion.TORCH_ENTRY)
            mUseSliderTorch = false
        } else if (mUseSliderTorch) {
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

    private fun launchSpecialActions(value: String?): Boolean {
        val musicPlaybackEnabled = Settings.System.getIntForUser(mContext.getContentResolver(),
                "Settings.System." + DeviceSettings.Companion.GESTURE_MUSIC_PLAYBACK_SETTINGS_VARIABLE_NAME, 0, UserHandle.USER_CURRENT) === 1
        /* handle music playback gesture if enabled */if (musicPlaybackEnabled) {
            when (value) {
                AppSelectListPreference.Companion.MUSIC_PLAY_ENTRY -> {
                    mGestureWakeLock.acquire(Constants.GESTURE_WAKELOCK_DURATION)
                    AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false, mContext,
                            DeviceSettings.Companion.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                            Constants.GESTURE_HAPTIC_DURATION)
                    dispatchMediaKeyWithWakeLockToAudioService(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                    return true
                }
                AppSelectListPreference.Companion.MUSIC_NEXT_ENTRY -> {
                    if (isMusicActive) {
                        mGestureWakeLock.acquire(Constants.GESTURE_WAKELOCK_DURATION)
                        AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false, mContext,
                                DeviceSettings.Companion.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                                Constants.GESTURE_HAPTIC_DURATION)
                        dispatchMediaKeyWithWakeLockToAudioService(KeyEvent.KEYCODE_MEDIA_NEXT)
                    }
                    return true
                }
                AppSelectListPreference.Companion.MUSIC_PREV_ENTRY -> {
                    if (isMusicActive) {
                        mGestureWakeLock.acquire(Constants.GESTURE_WAKELOCK_DURATION)
                        AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false, mContext,
                                DeviceSettings.Companion.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                                Constants.GESTURE_HAPTIC_DURATION)
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
                    /*if (mUseSliderTorch) {
                        service.toggleCameraFlashState(mTorchState);
                        return true;
                    } else {*/
                    service.toggleCameraFlash()
                    AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false, mContext,
                            DeviceSettings.Companion.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                            Constants.GESTURE_HAPTIC_DURATION)
                    return true
                    // }
                } catch (e: RemoteException) {
                    // do nothing.
                }
            }
        } else if (value == AppSelectListPreference.Companion.AMBIENT_DISPLAY_ENTRY) {
            AicpVibe.performHapticFeedbackLw(HapticFeedbackConstants.LONG_PRESS, false, mContext,
                    DeviceSettings.Companion.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                    Constants.GESTURE_HAPTIC_DURATION)
            launchDozePulse()
            return true
        }
        return false
    }

    private fun onDisplayOn() {
        if (DEBUG) Log.i(TAG, "Display on")
    }

    private fun onDisplayOff() {
        if (DEBUG) Log.i(TAG, "Display off")
    }

    private fun launchDozePulse() {
        // Note: Only works with ambient display enabled.
        mContext.sendBroadcastAsUser(Intent(Constants.DOZE_INTENT),
                UserHandle(UserHandle.USER_CURRENT))
    }

    val statusBarService: IStatusBarService
        get() = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"))

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

    val isMusicActive: Boolean
        get() = mAudioManager.isMusicActive()

    private val audioService: IAudioService?
        private get() {
            val audioService: IAudioService = IAudioService.Stub
                    .asInterface(ServiceManager.checkService(Context.AUDIO_SERVICE))
            if (audioService == null) {
                Log.w(TAG, "Unable to find IAudioService interface.")
            }
            return audioService
        }

    companion object {
        private const val TAG = "KeyHandler"
        private const val DEBUG = false
        private const val DEBUG_SENSOR = false
        private const val KEY_CONTROL_PATH = "/proc/touchpanel/key_disable"
        private const val mButtonDisabled = false
    }

    init {
        mContext = context
        mDispOn = true
        mEventHandler = EventHandler()
        mPowerManager = mContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        mGestureWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "GestureWakeLock")
        mNotificationManager = mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mAudioManager = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val screenStateFilter = IntentFilter(Intent.ACTION_SCREEN_ON)
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF)
        mContext.registerReceiver(mScreenStateReceiver, screenStateFilter)
    }
}