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
import android.content.Context
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener

class SingleTapSwitch(context: Context) : OnPreferenceChangeListener {
    private val mContext: Context
    fun onPreferenceChange(preference: Preference?, newValue: Any): Boolean {
        val enabled = newValue as Boolean
        Settings.System.putInt(mContext.getContentResolver(), SETTINGS_KEY, if (enabled) 1 else 0)
        Utils.writeValue(getFile(mContext), if (enabled) "1" else "0")
        return true
    }

    companion object {
        val SETTINGS_KEY: String = DeviceSettings.Companion.KEY_SETTINGS_PREFIX + DeviceSettings.Companion.KEY_STAP_SWITCH
        fun getFile(context: Context): String? {
            val fileName: String = context.getResources().getString(R.string.pathSTapToggle)
            return if (fileName != null && !fileName.isEmpty() && Utils.fileWritable(fileName)) {
                fileName
            } else null
        }

        fun isSupported(context: Context): Boolean {
            return getFile(context) != null
        }

        fun isCurrentlyEnabled(context: Context): Boolean {
            return Utils.getFileValueAsBoolean(getFile(context), false)
        }
    }

    init {
        mContext = context
    }
}