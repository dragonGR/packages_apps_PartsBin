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
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragment
import androidx.preference.PreferenceScreen
import androidx.preference.TwoStatePreference
import com.android.internal.util.aicp.PackageUtils

class DeviceSettings : PreferenceFragment(), Preference.OnPreferenceChangeListener {
    private var mVibratorSystemStrength: VibratorSystemStrengthPreference? = null
    private var mVibratorCallStrength: VibratorCallStrengthPreference? = null
    private var mVibratorNotifStrength: VibratorNotifStrengthPreference? = null
    private var mEarpieceGainPref: EarpieceGainPreference? = null
    private var mHeadphoneGainPref: HeadphoneGainPreference? = null
    private var mMicGainPref: MicGainPreference? = null
    private var mSpeakerGainPref: SpeakerGainPreference? = null
    private var mSliderModeTop: ListPreference? = null
    private var mSliderModeCenter: ListPreference? = null
    private var mSliderModeBottom: ListPreference? = null
    private var mOffScreenGestures: Preference? = null
    private var mPanelSettings: Preference? = null
    fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main, rootKey)
        val hasAlertSlider: Boolean = getContext().getResources().getBoolean(com.android.internal.R.bool.config_hasAlertSlider)
        val supportsGestures: Boolean = getContext().getResources().getBoolean(R.bool.config_device_supports_gestures)
        val supportsPanels: Boolean = getContext().getResources().getBoolean(R.bool.config_device_supports_panels)
        if (hasAlertSlider) {
            mSliderModeTop = findPreference(KEY_SLIDER_MODE_TOP) as ListPreference?
            mSliderModeTop.setOnPreferenceChangeListener(this)
            val sliderModeTop = getSliderAction(0)
            var valueIndex: Int = mSliderModeTop.findIndexOfValue(sliderModeTop.toString())
            mSliderModeTop.setValueIndex(valueIndex)
            mSliderModeTop.setSummary(mSliderModeTop.getEntries().get(valueIndex))
            mSliderModeCenter = findPreference(KEY_SLIDER_MODE_CENTER) as ListPreference?
            mSliderModeCenter.setOnPreferenceChangeListener(this)
            val sliderModeCenter = getSliderAction(1)
            valueIndex = mSliderModeCenter.findIndexOfValue(sliderModeCenter.toString())
            mSliderModeCenter.setValueIndex(valueIndex)
            mSliderModeCenter.setSummary(mSliderModeCenter.getEntries().get(valueIndex))
            mSliderModeBottom = findPreference(KEY_SLIDER_MODE_BOTTOM) as ListPreference?
            mSliderModeBottom.setOnPreferenceChangeListener(this)
            val sliderModeBottom = getSliderAction(2)
            valueIndex = mSliderModeBottom.findIndexOfValue(sliderModeBottom.toString())
            mSliderModeBottom.setValueIndex(valueIndex)
            mSliderModeBottom.setSummary(mSliderModeBottom.getEntries().get(valueIndex))
        } else {
            val sliderCategory: PreferenceCategory = findPreference(KEY_SLIDER_CATEGORY) as PreferenceCategory
            sliderCategory.getParent().removePreference(sliderCategory)
        }
        mHWKSwitch = findPreference(KEY_HWK_SWITCH) as TwoStatePreference?
        if (mHWKSwitch != null && HWKSwitch.Companion.isSupported()) {
            mHWKSwitch.setEnabled(true)
            mHWKSwitch.setChecked(HWKSwitch.Companion.isCurrentlyEnabled())
            mHWKSwitch.setOnPreferenceChangeListener(HWKSwitch(getContext()))
        } else {
            val buttonsCategory: PreferenceCategory = findPreference(KEY_BUTTON_CATEGORY) as PreferenceCategory
            buttonsCategory.getParent().removePreference(buttonsCategory)
        }
        val gesturesCategory: PreferenceCategory = findPreference(KEY_GESTURES_CATEGORY) as PreferenceCategory
        mOffScreenGestures = findPreference(KEY_OFFSCREEN_GESTURES) as Preference?
        var gesturesRemoved = 0
        mSTapSwitch = findPreference(KEY_STAP_SWITCH) as TwoStatePreference?
        if (mSTapSwitch != null && SingleTapSwitch.Companion.isSupported(getContext())) {
            mSTapSwitch.setEnabled(true)
            mSTapSwitch.setChecked(SingleTapSwitch.Companion.isCurrentlyEnabled(getContext()))
            mSTapSwitch.setOnPreferenceChangeListener(SingleTapSwitch(getContext()))
        } else {
            gesturesCategory.removePreference(mSTapSwitch)
            gesturesRemoved += 1
        }
        mDoubleTapToWakeSwitch = findPreference(KEY_DT2W_SWITCH) as TwoStatePreference?
        if (mDoubleTapToWakeSwitch != null && DoubleTapToWakeSwitch.Companion.isSupported(getContext())) {
            mDoubleTapToWakeSwitch.setEnabled(true)
            mDoubleTapToWakeSwitch.setChecked(DoubleTapToWakeSwitch.Companion.isCurrentlyEnabled(getContext()))
            mDoubleTapToWakeSwitch.setOnPreferenceChangeListener(DoubleTapToWakeSwitch(getContext()))
        } else {
            gesturesCategory.removePreference(mDoubleTapToWakeSwitch)
            gesturesRemoved += 1
        }
        mSweepToWakeSwitch = findPreference(KEY_S2W_SWITCH) as TwoStatePreference?
        if (mSweepToWakeSwitch != null && SweepToWakeSwitch.Companion.isSupported(getContext())) {
            mSweepToWakeSwitch.setEnabled(true)
            mSweepToWakeSwitch.setChecked(SweepToWakeSwitch.Companion.isCurrentlyEnabled(getContext()))
            mSweepToWakeSwitch.setOnPreferenceChangeListener(SweepToWakeSwitch(getContext()))
        } else {
            gesturesCategory.removePreference(mSweepToWakeSwitch)
            gesturesRemoved += 1
        }
        if (!supportsGestures) {
            mOffScreenGestures.getParent().removePreference(mOffScreenGestures)
            gesturesRemoved += 1
        }
        if (gesturesRemoved == 4) gesturesCategory.getParent().removePreference(gesturesCategory)
        val graphicsCategory: PreferenceCategory = findPreference(KEY_GRAPHICS_CATEGORY) as PreferenceCategory
        mPanelSettings = findPreference(KEY_PANEL_SETTINGS) as Preference?
        var graphicsRemoved = 0
        mHBMModeSwitch = findPreference(KEY_HBM_SWITCH) as TwoStatePreference?
        if (mHBMModeSwitch != null && HBMModeSwitch.Companion.isSupported(getContext())) {
            mHBMModeSwitch.setEnabled(true)
            mHBMModeSwitch.setChecked(HBMModeSwitch.Companion.isCurrentlyEnabled(getContext()))
            mHBMModeSwitch.setOnPreferenceChangeListener(HBMModeSwitch(getContext()))
        } else {
            graphicsCategory.removePreference(mHBMModeSwitch)
            graphicsRemoved += 1
        }
        mDCDModeSwitch = findPreference(KEY_DCD_SWITCH) as TwoStatePreference?
        if (mDCDModeSwitch != null && DCDModeSwitch.Companion.isSupported(getContext())) {
            mDCDModeSwitch.setEnabled(true)
            mDCDModeSwitch.setChecked(DCDModeSwitch.Companion.isCurrentlyEnabled(getContext()))
            mDCDModeSwitch.setOnPreferenceChangeListener(DCDModeSwitch(getContext()))
        } else {
            graphicsCategory.removePreference(mDCDModeSwitch)
            graphicsRemoved += 1
        }
        if (!supportsPanels) {
            mPanelSettings.getParent().removePreference(mPanelSettings)
            graphicsRemoved += 1
        }
        if (graphicsRemoved == 3) graphicsCategory.getParent().removePreference(graphicsCategory)
        val powerCategory: PreferenceCategory = findPreference(KEY_POWER_CATEGORY) as PreferenceCategory
        mFastChargeSwitch = findPreference(KEY_FASTCHARGE_SWITCH) as TwoStatePreference?
        if (mFastChargeSwitch != null && FastChargeSwitch.Companion.isSupported(getContext())) {
            mFastChargeSwitch.setEnabled(true)
            mFastChargeSwitch.setChecked(FastChargeSwitch.Companion.isCurrentlyEnabled(getContext()))
            mFastChargeSwitch.setOnPreferenceChangeListener(FastChargeSwitch(getContext()))
        } else {
            powerCategory.removePreference(mFastChargeSwitch)
            powerCategory.getParent().removePreference(powerCategory)
        }
        val audiogainsCategory: PreferenceCategory = findPreference(KEY_AUDIOGAINS_CATEGORY) as PreferenceCategory
        var audiogainsRemoved = 0
        mEarpieceGainPref = findPreference(KEY_EARPIECE_GAIN) as EarpieceGainPreference?
        if (mEarpieceGainPref != null && EarpieceGainPreference.Companion.isSupported()) {
            mEarpieceGainPref.setEnabled(true)
        } else {
            mEarpieceGainPref.getParent().removePreference(mEarpieceGainPref)
            audiogainsRemoved += 1
        }
        mHeadphoneGainPref = findPreference(KEY_HEADPHONE_GAIN) as HeadphoneGainPreference?
        if (mHeadphoneGainPref != null && HeadphoneGainPreference.Companion.isSupported()) {
            mHeadphoneGainPref.setEnabled(true)
        } else {
            mHeadphoneGainPref.getParent().removePreference(mHeadphoneGainPref)
            audiogainsRemoved += 1
        }
        mMicGainPref = findPreference(KEY_MIC_GAIN) as MicGainPreference?
        if (mMicGainPref != null && MicGainPreference.Companion.isSupported()) {
            mMicGainPref.setEnabled(true)
        } else {
            mMicGainPref.getParent().removePreference(mMicGainPref)
            audiogainsRemoved += 1
        }
        mSpeakerGainPref = findPreference(KEY_SPEAKER_GAIN) as SpeakerGainPreference?
        if (mSpeakerGainPref != null && SpeakerGainPreference.Companion.isSupported()) {
            mSpeakerGainPref.setEnabled(true)
        } else {
            mSpeakerGainPref.getParent().removePreference(mSpeakerGainPref)
            audiogainsRemoved += 1
        }
        if (audiogainsRemoved == 4) audiogainsCategory.getParent().removePreference(audiogainsCategory)
        val vibratorCategory: PreferenceCategory = findPreference(KEY_VIBRATOR_CATEGORY) as PreferenceCategory
        var countVibRemoved = 0
        mVibratorSystemStrength = findPreference(KEY_SYSTEM_VIBSTRENGTH) as VibratorSystemStrengthPreference?
        if (mVibratorSystemStrength != null && mVibratorSystemStrength!!.isSupported) {
            mVibratorSystemStrength.setEnabled(true)
        } else {
            mVibratorSystemStrength.getParent().removePreference(mVibratorSystemStrength)
            countVibRemoved += 1
        }
        mVibratorCallStrength = findPreference(KEY_CALL_VIBSTRENGTH) as VibratorCallStrengthPreference?
        if (mVibratorCallStrength != null && mVibratorCallStrength!!.isSupported) {
            mVibratorCallStrength.setEnabled(true)
        } else {
            mVibratorCallStrength.getParent().removePreference(mVibratorCallStrength)
            countVibRemoved += 1
        }
        mVibratorNotifStrength = findPreference(KEY_NOTIF_VIBSTRENGTH) as VibratorNotifStrengthPreference?
        if (mVibratorNotifStrength != null && mVibratorNotifStrength!!.isSupported) {
            mVibratorNotifStrength.setEnabled(true)
        } else {
            mVibratorNotifStrength.getParent().removePreference(mVibratorNotifStrength)
            countVibRemoved += 1
        }
        if (countVibRemoved == 3) vibratorCategory.getParent().removePreference(vibratorCategory)
    }

    fun onPreferenceTreeClick(preference: Preference?): Boolean {
        return super.onPreferenceTreeClick(preference)
    }

    fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        if (preference === mSliderModeTop) {
            val value = newValue as String?
            val sliderMode = Integer.valueOf(value)
            setSliderAction(0, sliderMode)
            val valueIndex: Int = mSliderModeTop.findIndexOfValue(value)
            mSliderModeTop.setSummary(mSliderModeTop.getEntries().get(valueIndex))
        } else if (preference === mSliderModeCenter) {
            val value = newValue as String?
            val sliderMode = Integer.valueOf(value)
            setSliderAction(1, sliderMode)
            val valueIndex: Int = mSliderModeCenter.findIndexOfValue(value)
            mSliderModeCenter.setSummary(mSliderModeCenter.getEntries().get(valueIndex))
        } else if (preference === mSliderModeBottom) {
            val value = newValue as String?
            val sliderMode = Integer.valueOf(value)
            setSliderAction(2, sliderMode)
            val valueIndex: Int = mSliderModeBottom.findIndexOfValue(value)
            mSliderModeBottom.setSummary(mSliderModeBottom.getEntries().get(valueIndex))
        }
        return true
    }

    private fun getSliderAction(position: Int): Int {
        var value: String? = Settings.System.getString(getContext().getContentResolver(),
                Settings.System.OMNI_BUTTON_EXTRA_KEY_MAPPING)
        val defaultValue = SLIDER_DEFAULT_VALUE
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

    private fun setSliderAction(position: Int, action: Int) {
        var value: String? = Settings.System.getString(getContext().getContentResolver(),
                Settings.System.OMNI_BUTTON_EXTRA_KEY_MAPPING)
        val defaultValue = SLIDER_DEFAULT_VALUE
        if (value == null) {
            value = defaultValue
        } else if (value.indexOf(",") == -1) {
            value = defaultValue
        }
        try {
            val parts = value.split(",".toRegex()).toTypedArray()
            parts[position] = action.toString()
            val newValue: String = TextUtils.join(",", parts)
            Settings.System.putString(getContext().getContentResolver(),
                    Settings.System.OMNI_BUTTON_EXTRA_KEY_MAPPING, newValue)
        } catch (e: Exception) {
        }
    }

    companion object {
        const val GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME = "OFF_GESTURE_HAPTIC_ENABLE"
        const val GESTURE_MUSIC_PLAYBACK_SETTINGS_VARIABLE_NAME = "MUSIC_PLAYBACK_GESTURE_ENABLE"
        const val KEY_SYSTEM_VIBSTRENGTH = "vib_system_strength"
        const val KEY_CALL_VIBSTRENGTH = "vib_call_strength"
        const val KEY_NOTIF_VIBSTRENGTH = "vib_notif_strength"
        private const val KEY_SLIDER_MODE_TOP = "slider_mode_top"
        private const val KEY_SLIDER_MODE_CENTER = "slider_mode_center"
        private const val KEY_SLIDER_MODE_BOTTOM = "slider_mode_bottom"
        private const val KEY_BUTTON_CATEGORY = "category_buttons"
        private const val KEY_GRAPHICS_CATEGORY = "category_graphics"
        private const val KEY_VIBRATOR_CATEGORY = "category_vibrator"
        private const val KEY_SLIDER_CATEGORY = "category_slider"
        private const val KEY_GESTURES_CATEGORY = "category_gestures"
        private const val KEY_POWER_CATEGORY = "category_power"
        private const val KEY_AUDIOGAINS_CATEGORY = "category_audiogains"
        const val KEY_HEADPHONE_GAIN = "headphone_gain"
        const val KEY_EARPIECE_GAIN = "earpiece_gain"
        const val KEY_MIC_GAIN = "mic_gain"
        const val KEY_SPEAKER_GAIN = "speaker_gain"
        const val KEY_SRGB_SWITCH = "srgb"
        const val KEY_HBM_SWITCH = "hbm"
        const val KEY_PROXI_SWITCH = "proxi"
        const val KEY_DCD_SWITCH = "dcd"
        const val KEY_DCI_SWITCH = "dci"
        const val KEY_WIDE_SWITCH = "wide"
        const val KEY_ONEPLUSMODE_SWITCH = "oneplus"
        const val KEY_HWK_SWITCH = "hwk"
        const val KEY_STAP_SWITCH = "single_tap"
        const val KEY_DT2W_SWITCH = "double_tap_to_wake"
        const val KEY_S2W_SWITCH = "sweep_to_wake"
        const val KEY_FASTCHARGE_SWITCH = "fastcharge"
        const val KEY_OFFSCREEN_GESTURES = "gesture_category"
        const val KEY_PANEL_SETTINGS = "panel_category"
        const val SLIDER_DEFAULT_VALUE = "2,1,0"
        const val KEY_SETTINGS_PREFIX = "device_setting_"
        private var mHBMModeSwitch: TwoStatePreference? = null
        private var mDCDModeSwitch: TwoStatePreference? = null
        private var mHWKSwitch: TwoStatePreference? = null
        private var mSTapSwitch: TwoStatePreference? = null
        private var mFastChargeSwitch: TwoStatePreference? = null
        private var mDoubleTapToWakeSwitch: TwoStatePreference? = null
        private var mSweepToWakeSwitch: TwoStatePreference? = null
    }
}