/*
* Copyright (C) 2016 The OmniROM Project
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
import android.database.ContentObserver
import android.os.Bundle
import android.provider.Settings
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

class SpeakerGainPreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs), SeekBar.OnSeekBarChangeListener {
    private var mMinValue = 0
    private var mMaxValue = 1
    private var mSeekBar: SeekBar? = null
    private var mOldStrength = 0
    fun getValue(context: Context?): String? {
        return Utils.getFileValue(mFileName, DEFAULT_VALUE)
    }

    private fun setValue(newValue: String) {
        Log.d(TAG, "setValue - mFileName $mFileName - newValue $newValue")
        Utils.writeValue(mFileName, newValue)
        Settings.System.putString(getContext().getContentResolver(), SETTINGS_KEY, newValue)
    }

    private fun restore(context: Context) {
        if (!isSupported) {
            return
        }
        var storedValue: String = Settings.System.getString(context.getContentResolver(), SETTINGS_KEY)
        if (storedValue == null) {
            storedValue = DEFAULT_VALUE
        }
        Utils.writeValue(mFileName, storedValue)
    }

    fun onProgressChanged(seekBar: SeekBar?, progress: Int,
                          fromTouch: Boolean) {
        setValue((progress + mMinValue).toString())
        Log.d(TAG, "onProgressChanged - progress $progress - mMinValue $mMinValue")
    }

    fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        mOldStrength = getValue(getContext())!!.toInt()
        mSeekBar = holder.findViewById(R.id.seekbar) as SeekBar
        mSeekBar.setMax(mMaxValue - mMinValue)
        mSeekBar.setProgress(mOldStrength - mMinValue)
        mSeekBar.setOnSeekBarChangeListener(this)
    }

    fun onStartTrackingTouch(seekBar: SeekBar?) {
        // NA
    }

    fun onStopTrackingTouch(seekBar: SeekBar?) {
        // NA
    }

    companion object {
        private const val TAG = "SpeakerGainPreference"
        var SETTINGS_KEY: String = DeviceSettings.Companion.KEY_SETTINGS_PREFIX + DeviceSettings.Companion.KEY_SPEAKER_GAIN
        protected var DEFAULT_VALUE: String
        private var mFileName: String?
        val isSupported: Boolean
            get() = if (mFileName != null && !mFileName!!.isEmpty()) {
                Utils.fileWritable(mFileName)
            } else false

        fun getFile(context: Context): String? {
            mFileName = context.getResources().getString(R.string.pathAudioSpeakerGain)
            return if (isSupported) {
                mFileName
            } else null
        }

        fun getDefaultValue(context: Context): String {
            return if (isSupported) Integer.toString(context.getResources().getInteger(R.integer.audioSpeakerGainDefault)) else Integer.toString(0)
        }
    }

    init {
        // from sound/soc/codecs/wcd9335.c
        mFileName = context.getResources().getString(R.string.pathAudioSpeakerGain)
        if (isSupported) {
            mMinValue = context.getResources().getInteger(R.integer.audioSpeakerGainMin)
            mMaxValue = context.getResources().getInteger(R.integer.audioSpeakerGainMax)
        }
        DEFAULT_VALUE = getDefaultValue(context)
        setLayoutResource(R.layout.preference_seek_bar)
        restore(context)
    }
}