/*
* Copyright (C) 2013 The OmniROM Project
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
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.provider.Settings
import android.text.TextUtils
import androidx.preference.PreferenceManager
import com.aicp.device.AppSelectListPreference

class Startup : BroadcastReceiver() {
    private fun maybeImportOldSettings(context: Context) {
        val resolver: ContentResolver = context.getContentResolver()
        val imported = Settings.System.getInt(resolver, "omni_device_setting_imported", 0) !== 0
        if (!imported) {
            val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            var enabled: Boolean = sharedPrefs.getBoolean(DeviceSettings.Companion.KEY_SRGB_SWITCH, false)
            Settings.System.putInt(resolver, SRGBModeSwitch.SETTINGS_KEY, if (enabled) 1 else 0)
            enabled = sharedPrefs.getBoolean(DeviceSettings.Companion.KEY_HBM_SWITCH, false)
            Settings.System.putInt(resolver, HBMModeSwitch.Companion.SETTINGS_KEY, if (enabled) 1 else 0)
            enabled = sharedPrefs.getBoolean(DeviceSettings.Companion.KEY_STAP_SWITCH, false)
            Settings.System.putInt(resolver, SingleTapSwitch.Companion.SETTINGS_KEY, if (enabled) 1 else 0)
            enabled = sharedPrefs.getBoolean(DeviceSettings.Companion.KEY_FASTCHARGE_SWITCH, false)
            Settings.System.putInt(resolver, FastChargeSwitch.Companion.SETTINGS_KEY, if (enabled) 1 else 0)
            enabled = sharedPrefs.getBoolean(DeviceSettings.Companion.KEY_DT2W_SWITCH, false)
            Settings.System.putInt(resolver, DoubleTapToWakeSwitch.Companion.SETTINGS_KEY, if (enabled) 1 else 0)
            enabled = sharedPrefs.getBoolean(DeviceSettings.Companion.KEY_S2W_SWITCH, false)
            Settings.System.putInt(resolver, SweepToWakeSwitch.Companion.SETTINGS_KEY, if (enabled) 1 else 0)
            enabled = sharedPrefs.getBoolean(DeviceSettings.Companion.KEY_DCD_SWITCH, false)
            Settings.System.putInt(resolver, DCDModeSwitch.Companion.SETTINGS_KEY, if (enabled) 1 else 0)
            enabled = sharedPrefs.getBoolean(DeviceSettings.Companion.KEY_DCI_SWITCH, false)
            Settings.System.putInt(resolver, DCIModeSwitch.SETTINGS_KEY, if (enabled) 1 else 0)
            enabled = sharedPrefs.getBoolean(DeviceSettings.Companion.KEY_WIDE_SWITCH, false)
            Settings.System.putInt(resolver, WideModeSwitch.SETTINGS_KEY, if (enabled) 1 else 0)
            val vibrSystemStrength: String = sharedPrefs.getString(DeviceSettings.Companion.KEY_SYSTEM_VIBSTRENGTH, VibratorSystemStrengthPreference.Companion.getDefaultValue(context))
            Settings.System.putString(resolver, VibratorSystemStrengthPreference.Companion.SETTINGS_KEY, vibrSystemStrength)
            val vibrCallStrength: String = sharedPrefs.getString(DeviceSettings.Companion.KEY_CALL_VIBSTRENGTH, VibratorCallStrengthPreference.Companion.getDefaultValue(context))
            Settings.System.putString(resolver, VibratorCallStrengthPreference.Companion.SETTINGS_KEY, vibrCallStrength)
            val vibrNotifStrength: String = sharedPrefs.getString(DeviceSettings.Companion.KEY_NOTIF_VIBSTRENGTH, VibratorNotifStrengthPreference.Companion.getDefaultValue(context))
            Settings.System.putString(resolver, VibratorNotifStrengthPreference.Companion.SETTINGS_KEY, vibrNotifStrength)
            val audioEarpieceGain: String = sharedPrefs.getString(DeviceSettings.Companion.KEY_EARPIECE_GAIN, EarpieceGainPreference.Companion.getDefaultValue(context))
            Settings.System.putString(resolver, EarpieceGainPreference.Companion.SETTINGS_KEY, audioEarpieceGain)
            restore(EarpieceGainPreference.Companion.getFile(context), audioEarpieceGain)
            val audioMicGain: String = sharedPrefs.getString(DeviceSettings.Companion.KEY_MIC_GAIN, MicGainPreference.Companion.getDefaultValue(context))
            Settings.System.putString(resolver, MicGainPreference.Companion.SETTINGS_KEY, audioMicGain)
            restore(MicGainPreference.Companion.getFile(context), audioMicGain)
            val audioSpeakerGain: String = sharedPrefs.getString(DeviceSettings.Companion.KEY_SPEAKER_GAIN, SpeakerGainPreference.Companion.getDefaultValue(context))
            Settings.System.putString(resolver, SpeakerGainPreference.Companion.SETTINGS_KEY, audioSpeakerGain)
            restore(SpeakerGainPreference.Companion.getFile(context), audioSpeakerGain)
            val audioHeadphoneGain: String = sharedPrefs.getString(DeviceSettings.Companion.KEY_HEADPHONE_GAIN, HeadphoneGainPreference.Companion.getDefaultValue(context))
            Settings.System.putString(resolver, HeadphoneGainPreference.Companion.SETTINGS_KEY, audioHeadphoneGain)
            restoreDual(HeadphoneGainPreference.Companion.getFile(context), audioHeadphoneGain)
            Settings.System.putInt(resolver, "omni_device_setting_imported", 1)
        }
    }

    fun onReceive(context: Context, bootintent: Intent?) {
        maybeImportOldSettings(context)
        restoreAfterUserSwitch(context)
    }

    companion object {
        private fun restore(file: String?, enabled: Boolean) {
            if (file == null) {
                return
            }
            Utils.writeValue(file, if (enabled) "1" else "0")
        }

        private fun restore(file: String?, value: String) {
            if (file == null) {
                return
            }
            Utils.writeValue(file, value)
        }

        private fun restoreDual(file: String?, value: String) {
            if (file == null) {
                return
            }
            Utils.writeValueDual(file, value)
        }

        private fun getGestureFile(key: String): String? {
            return GestureSettings.Companion.getGestureFile(key)
        }

        fun restoreAfterUserSwitch(context: Context) {
            val supportsGestures: Boolean = context.getResources().getBoolean(R.bool.config_device_supports_gestures)
            val resolver: ContentResolver = context.getContentResolver()
            var enabled: Boolean
            if (supportsGestures) {
                // music playback
                val musicPlaybackEnabled = Settings.System.getInt(resolver,
                        "Settings.System." + DeviceSettings.Companion.GESTURE_MUSIC_PLAYBACK_SETTINGS_VARIABLE_NAME, 0) === 1
                restore(getGestureFile(GestureSettings.Companion.KEY_MUSIC_START), musicPlaybackEnabled)
                restore(getGestureFile(GestureSettings.Companion.KEY_MUSIC_TRACK_NEXT), musicPlaybackEnabled)
                restore(getGestureFile(GestureSettings.Companion.KEY_MUSIC_TRACK_PREV), musicPlaybackEnabled)

                // circle -> camera
                var mapping: String = GestureSettings.Companion.DEVICE_GESTURE_MAPPING_1
                var value: String = Settings.System.getString(resolver, mapping)
                if (TextUtils.isEmpty(value)) {
                    value = AppSelectListPreference.Companion.CAMERA_ENTRY
                    Settings.System.putString(resolver, mapping, value)
                }
                enabled = value != AppSelectListPreference.Companion.DISABLED_ENTRY
                restore(getGestureFile(GestureSettings.Companion.KEY_CIRCLE_APP), enabled)

                // down arrow -> flashlight
                mapping = GestureSettings.Companion.DEVICE_GESTURE_MAPPING_2
                value = Settings.System.getString(resolver, mapping)
                if (TextUtils.isEmpty(value)) {
                    value = AppSelectListPreference.Companion.TORCH_ENTRY
                    Settings.System.putString(resolver, mapping, value)
                }
                enabled = value != AppSelectListPreference.Companion.DISABLED_ENTRY
                restore(getGestureFile(GestureSettings.Companion.KEY_DOWN_ARROW_APP), enabled)

                // M Gesture
                value = Settings.System.getString(resolver, GestureSettings.Companion.DEVICE_GESTURE_MAPPING_3)
                enabled = !TextUtils.isEmpty(value) && value != AppSelectListPreference.Companion.DISABLED_ENTRY
                restore(getGestureFile(GestureSettings.Companion.KEY_M_GESTURE_APP), enabled)

                // down swipe
                value = Settings.System.getString(resolver, GestureSettings.Companion.DEVICE_GESTURE_MAPPING_6)
                enabled = !TextUtils.isEmpty(value) && value != AppSelectListPreference.Companion.DISABLED_ENTRY
                restore(getGestureFile(GestureSettings.Companion.KEY_DOWN_SWIPE_APP), enabled)

                // up swipe
                value = Settings.System.getString(resolver, GestureSettings.Companion.DEVICE_GESTURE_MAPPING_7)
                enabled = !TextUtils.isEmpty(value) && value != AppSelectListPreference.Companion.DISABLED_ENTRY
                restore(getGestureFile(GestureSettings.Companion.KEY_UP_SWIPE_APP), enabled)

                // left swipe
                value = Settings.System.getString(resolver, GestureSettings.Companion.DEVICE_GESTURE_MAPPING_8)
                enabled = !TextUtils.isEmpty(value) && value != AppSelectListPreference.Companion.DISABLED_ENTRY
                restore(getGestureFile(GestureSettings.Companion.KEY_LEFT_SWIPE_APP), enabled)

                // right swipe
                value = Settings.System.getString(resolver, GestureSettings.Companion.DEVICE_GESTURE_MAPPING_9)
                enabled = !TextUtils.isEmpty(value) && value != AppSelectListPreference.Companion.DISABLED_ENTRY
                restore(getGestureFile(GestureSettings.Companion.KEY_RIGHT_SWIPE_APP), enabled)

                // S Gesture
                value = Settings.System.getString(resolver, GestureSettings.Companion.DEVICE_GESTURE_MAPPING_10)
                enabled = !TextUtils.isEmpty(value) && value != AppSelectListPreference.Companion.DISABLED_ENTRY
                restore(getGestureFile(GestureSettings.Companion.KEY_S_GESTURE_APP), enabled)

                // W Gesture
                value = Settings.System.getString(resolver, GestureSettings.Companion.DEVICE_GESTURE_MAPPING_11)
                enabled = !TextUtils.isEmpty(value) && value != AppSelectListPreference.Companion.DISABLED_ENTRY
                restore(getGestureFile(GestureSettings.Companion.KEY_W_GESTURE_APP), enabled)
            }
            enabled = Settings.System.getInt(resolver, SRGBModeSwitch.SETTINGS_KEY, 0) !== 0
            restore(SRGBModeSwitch.getFile(context), enabled)
            enabled = Settings.System.getInt(resolver, DCDModeSwitch.Companion.SETTINGS_KEY, 0) !== 0
            restore(DCDModeSwitch.Companion.getFile(context), enabled)
            enabled = Settings.System.getInt(resolver, DCIModeSwitch.SETTINGS_KEY, 0) !== 0
            restore(DCIModeSwitch.getFile(context), enabled)
            enabled = Settings.System.getInt(resolver, WideModeSwitch.SETTINGS_KEY, 0) !== 0
            restore(WideModeSwitch.getFile(context), enabled)
            enabled = Settings.System.getInt(resolver, HBMModeSwitch.Companion.SETTINGS_KEY, 0) !== 0
            restore(HBMModeSwitch.Companion.getFile(context), enabled)
            enabled = Settings.System.getInt(resolver, FastChargeSwitch.Companion.SETTINGS_KEY, 0) !== 0
            restore(FastChargeSwitch.Companion.getFile(context), enabled)
            enabled = Settings.System.getInt(resolver, SingleTapSwitch.Companion.SETTINGS_KEY, 0) !== 0
            restore(SingleTapSwitch.Companion.getFile(context), enabled)
            enabled = Settings.System.getInt(resolver, DoubleTapToWakeSwitch.Companion.SETTINGS_KEY, 0) !== 0
            restore(DoubleTapToWakeSwitch.Companion.getFile(context), enabled)
            enabled = Settings.System.getInt(resolver, SweepToWakeSwitch.Companion.SETTINGS_KEY, 0) !== 0
            restore(SweepToWakeSwitch.Companion.getFile(context), enabled)
            restore(EarpieceGainPreference.Companion.getFile(context), Settings.System.getString(resolver,
                    EarpieceGainPreference.Companion.SETTINGS_KEY))
            restore(MicGainPreference.Companion.getFile(context), Settings.System.getString(resolver,
                    MicGainPreference.Companion.SETTINGS_KEY))
            restoreDual(HeadphoneGainPreference.Companion.getFile(context), Settings.System.getString(resolver,
                    HeadphoneGainPreference.Companion.SETTINGS_KEY))
            restore(SpeakerGainPreference.Companion.getFile(context), Settings.System.getString(resolver,
                    SpeakerGainPreference.Companion.SETTINGS_KEY))
        }
    }
}