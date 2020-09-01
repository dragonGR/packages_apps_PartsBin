/*
* Copyright (C) 2016 The OmniROM Project
* Copyright (C) 20202 The Android Ice Cold Project
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
import android.database.ContentObserver
import android.os.Bundle
import android.os.Vibrator
import android.provider.Settings
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

abstract class VibratorStrengthPreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs), SeekBar.OnSeekBarChangeListener {
    private var mSeekBar: SeekBar? = null
    private var mOldStrength = 0
    protected var mMinValue = 0
    protected var mMaxValue = 0
    protected var mVibrator: Vibrator
    fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        mOldStrength = getValue(getContext())!!.toInt()
        mSeekBar = holder.findViewById(R.id.seekbar) as SeekBar
        mSeekBar.setMax(mMaxValue - mMinValue)
        mSeekBar.setProgress(mOldStrength - mMinValue)
        mSeekBar.setOnSeekBarChangeListener(this)
    }

    protected abstract fun getValue(context: Context?): String?
    protected abstract val isSupported: Boolean
    protected abstract fun setValue(newValue: String?, withFeedback: Boolean)
    fun onProgressChanged(seekBar: SeekBar?, progress: Int,
                          fromTouch: Boolean) {
        setValue((progress + mMinValue).toString(), true)
    }

    fun onStartTrackingTouch(seekBar: SeekBar?) {
        // NA
    }

    fun onStopTrackingTouch(seekBar: SeekBar?) {
        // NA
    }

    companion object {
        protected var testVibrationPattern: LongArray
        protected var SETTINGS_KEY: String? = null
        protected var DEFAULT_VALUE: String? = null
    }

    init {
        // from drivers/platform/msm/qpnp-haptic.c
        // #define QPNP_HAP_VMAX_MIN_MV		116
        // #define QPNP_HAP_VMAX_MAX_MV		3596
        mVibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        setLayoutResource(R.layout.preference_seek_bar)
    }
}