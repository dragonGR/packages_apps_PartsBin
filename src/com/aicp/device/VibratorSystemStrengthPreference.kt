/*
* Copyright (C) 2020 The Android Ice Cold Project
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
import android.content.ContentResolver
import android.content.Context
import android.os.Vibrator
import android.provider.Settings
import android.util.AttributeSet

class VibratorSystemStrengthPreference(context: Context, attrs: AttributeSet?) : VibratorStrengthPreference(context, attrs) {
    private val mFileName: String?
    override val isSupported: Boolean
        get() = if (mFileName != null && !mFileName.isEmpty()) {
            Utils.fileWritable(mFileName)
        } else false

    override fun getValue(context: Context?): String? {
        return Utils.getFileValue(mFileName, VibratorStrengthPreference.Companion.DEFAULT_VALUE)
    }

    override fun setValue(newValue: String?, withFeedback: Boolean) {
        Utils.writeValue(mFileName, newValue)
        Settings.System.putString(getContext().getContentResolver(), SETTINGS_KEY, newValue)
        if (withFeedback) {
            mVibrator.vibrate(testVibrationPattern, -1)
        }
    }

    private fun restore(context: Context) {
        if (!isSupported) {
            return
        }
        var storedValue: String = Settings.System.getString(context.getContentResolver(), SETTINGS_KEY)
        if (storedValue == null) {
            storedValue = VibratorStrengthPreference.Companion.DEFAULT_VALUE
        }
        Utils.writeValue(mFileName, storedValue)
    }

    companion object {
        protected var testVibrationPattern = longArrayOf(0, 250)
        var SETTINGS_KEY: String = DeviceSettings.Companion.KEY_SETTINGS_PREFIX + DeviceSettings.Companion.KEY_SYSTEM_VIBSTRENGTH
        fun getDefaultValue(context: Context): String {
            return Integer.toString(context.getResources().getInteger(R.integer.vibratorDefaultMV))
        }
    }

    init {
        // from drivers/platform/msm/qpnp-haptic.c
        // #define QPNP_HAP_VMAX_MIN_MV		116
        // #define QPNP_HAP_VMAX_MAX_MV		3596
        mFileName = context.getResources().getString(R.string.pathSystemVibStrength)
        mMinValue = context.getResources().getInteger(R.integer.vibratorMinMV)
        mMaxValue = context.getResources().getInteger(R.integer.vibratorMaxMV)
        VibratorStrengthPreference.Companion.DEFAULT_VALUE = getDefaultValue(context)
        setLayoutResource(R.layout.preference_seek_bar)
        restore(context)
    }
}