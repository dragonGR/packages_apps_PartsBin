/*
* Copyright (C) 2017 The OmniROM Project
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
import android.app.DialogFragment
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.os.AsyncTask
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import android.provider.Settings.Secure.SYSTEM_NAVIGATION_KEYS_ENABLED
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
import com.aicp.device.AppSelectListPreference
import com.aicp.device.AppSelectListPreference.AppSelectListPreferenceDialogFragment
import com.aicp.device.AppSelectListPreference.PackageItem
import java.util.*

class GestureSettings : PreferenceFragment(), Preference.OnPreferenceChangeListener {
    private var mMusicPlaybackGestureSwitch: TwoStatePreference? = null
    private var mOffscreenGestureFeedbackSwitch: TwoStatePreference? = null
    private val mDoubleSwipeApp: AppSelectListPreference? = null
    private var mCircleApp: AppSelectListPreference? = null
    private var mDownArrowApp: AppSelectListPreference? = null
    private val mUpArrowApp: AppSelectListPreference? = null
    private val mLeftArrowApp: AppSelectListPreference? = null
    private val mRightArrowApp: AppSelectListPreference? = null
    private var mDownSwipeApp: AppSelectListPreference? = null
    private var mUpSwipeApp: AppSelectListPreference? = null
    private var mLeftSwipeApp: AppSelectListPreference? = null
    private var mRightSwipeApp: AppSelectListPreference? = null
    private var mMGestureApp: AppSelectListPreference? = null
    private var mSGestureApp: AppSelectListPreference? = null
    private var mWGestureApp: AppSelectListPreference? = null
    private val mFPDownSwipeApp: AppSelectListPreference? = null
    private val mFPUpSwipeApp: AppSelectListPreference? = null
    private val mFPRightSwipeApp: AppSelectListPreference? = null
    private val mFPLeftSwipeApp: AppSelectListPreference? = null
    private val fpGestures: PreferenceCategory? = null
    private val mInstalledPackages: MutableList<PackageItem> = LinkedList()
    private var mPm: PackageManager? = null
    fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.gesture_settings, rootKey)
        mPm = getContext().getPackageManager()
        mOffscreenGestureFeedbackSwitch = findPreference(KEY_OFF_SCREEN_GESTURE_FEEDBACK_SWITCH) as TwoStatePreference?
        mOffscreenGestureFeedbackSwitch.setChecked(Settings.System.getInt(getContext().getContentResolver(),
                "Settings.System." + DeviceSettings.Companion.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME, 1) !== 0)
        mMusicPlaybackGestureSwitch = findPreference(KEY_MUSIC_START) as TwoStatePreference?
        mMusicPlaybackGestureSwitch.setChecked(Settings.System.getInt(getContext().getContentResolver(),
                "Settings.System." + DeviceSettings.Companion.GESTURE_MUSIC_PLAYBACK_SETTINGS_VARIABLE_NAME, 1) !== 0)
        val musicPlaybackEnabled = Settings.System.getIntForUser(getContext().getContentResolver(),
                "Settings.System." + DeviceSettings.Companion.GESTURE_MUSIC_PLAYBACK_SETTINGS_VARIABLE_NAME, 0, UserHandle.USER_CURRENT) === 1
        setMusicPlaybackGestureEnabled(musicPlaybackEnabled)
        mCircleApp = findPreference(KEY_CIRCLE_APP) as AppSelectListPreference?
        mCircleApp.setEnabled(isGestureSupported(KEY_CIRCLE_APP))
        var value: String = Settings.System.getString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_1)
        mCircleApp.setValue(value)
        mCircleApp.setOnPreferenceChangeListener(this)
        mDownArrowApp = findPreference(KEY_DOWN_ARROW_APP) as AppSelectListPreference?
        mDownArrowApp.setEnabled(isGestureSupported(KEY_DOWN_ARROW_APP))
        value = Settings.System.getString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_2)
        mDownArrowApp.setValue(value)
        mDownArrowApp.setOnPreferenceChangeListener(this)
        mMGestureApp = findPreference(KEY_M_GESTURE_APP) as AppSelectListPreference?
        mMGestureApp.setEnabled(isGestureSupported(KEY_M_GESTURE_APP))
        value = Settings.System.getString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_3)
        mMGestureApp.setValue(value)
        mMGestureApp.setOnPreferenceChangeListener(this)
        mDownSwipeApp = findPreference(KEY_DOWN_SWIPE_APP) as AppSelectListPreference?
        mDownSwipeApp.setEnabled(isGestureSupported(KEY_DOWN_SWIPE_APP))
        value = Settings.System.getString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_6)
        mDownSwipeApp.setValue(value)
        mDownSwipeApp.setOnPreferenceChangeListener(this)
        mUpSwipeApp = findPreference(KEY_UP_SWIPE_APP) as AppSelectListPreference?
        mUpSwipeApp.setEnabled(isGestureSupported(KEY_UP_SWIPE_APP))
        value = Settings.System.getString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_7)
        mUpSwipeApp.setValue(value)
        mUpSwipeApp.setOnPreferenceChangeListener(this)
        mLeftSwipeApp = findPreference(KEY_LEFT_SWIPE_APP) as AppSelectListPreference?
        mLeftSwipeApp.setEnabled(isGestureSupported(KEY_LEFT_SWIPE_APP))
        value = Settings.System.getString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_8)
        mLeftSwipeApp.setValue(value)
        mLeftSwipeApp.setOnPreferenceChangeListener(this)
        mRightSwipeApp = findPreference(KEY_RIGHT_SWIPE_APP) as AppSelectListPreference?
        mRightSwipeApp.setEnabled(isGestureSupported(KEY_RIGHT_SWIPE_APP))
        value = Settings.System.getString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_9)
        mRightSwipeApp.setValue(value)
        mRightSwipeApp.setOnPreferenceChangeListener(this)
        mSGestureApp = findPreference(KEY_S_GESTURE_APP) as AppSelectListPreference?
        mSGestureApp.setEnabled(isGestureSupported(KEY_S_GESTURE_APP))
        value = Settings.System.getString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_10)
        mSGestureApp.setValue(value)
        mSGestureApp.setOnPreferenceChangeListener(this)
        mWGestureApp = findPreference(KEY_W_GESTURE_APP) as AppSelectListPreference?
        mWGestureApp.setEnabled(isGestureSupported(KEY_W_GESTURE_APP))
        value = Settings.System.getString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_11)
        mWGestureApp.setValue(value)
        mWGestureApp.setOnPreferenceChangeListener(this)
        FetchPackageInformationTask().execute()
    }

    private fun areSystemNavigationKeysEnabled(): Boolean {
        return Settings.Secure.getInt(getContext().getContentResolver(),
                Settings.Secure.SYSTEM_NAVIGATION_KEYS_ENABLED, 0) === 1
    }

    fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference === mOffscreenGestureFeedbackSwitch) {
            Settings.System.putInt(getContext().getContentResolver(),
                    "Settings.System." + DeviceSettings.Companion.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME, if (mOffscreenGestureFeedbackSwitch.isChecked()) 1 else 0)
            return true
        } else if (preference === mMusicPlaybackGestureSwitch) {
            Settings.System.putInt(getContext().getContentResolver(),
                    "Settings.System." + DeviceSettings.Companion.GESTURE_MUSIC_PLAYBACK_SETTINGS_VARIABLE_NAME, if (mMusicPlaybackGestureSwitch.isChecked()) 1 else 0)
            setMusicPlaybackGestureEnabled(mMusicPlaybackGestureSwitch.isChecked())
            return true
        }
        return super.onPreferenceTreeClick(preference)
    }

    fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        if (preference === mCircleApp) {
            val value = newValue as String
            val gestureDisabled = value == AppSelectListPreference.Companion.DISABLED_ENTRY
            setGestureEnabled(KEY_CIRCLE_APP, !gestureDisabled)
            Settings.System.putString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_1, value)
        } else if (preference === mDownArrowApp) {
            val value = newValue as String
            val gestureDisabled = value == AppSelectListPreference.Companion.DISABLED_ENTRY
            setGestureEnabled(KEY_DOWN_ARROW_APP, !gestureDisabled)
            Settings.System.putString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_2, value)
        } else if (preference === mMGestureApp) {
            val value = newValue as String
            val gestureDisabled = value == AppSelectListPreference.Companion.DISABLED_ENTRY
            setGestureEnabled(KEY_M_GESTURE_APP, !gestureDisabled)
            Settings.System.putString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_3, value)
        } else if (preference === mDownSwipeApp) {
            val value = newValue as String
            val gestureDisabled = value == AppSelectListPreference.Companion.DISABLED_ENTRY
            setGestureEnabled(KEY_DOWN_SWIPE_APP, !gestureDisabled)
            Settings.System.putString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_6, value)
        } else if (preference === mUpSwipeApp) {
            val value = newValue as String
            val gestureDisabled = value == AppSelectListPreference.Companion.DISABLED_ENTRY
            setGestureEnabled(KEY_UP_SWIPE_APP, !gestureDisabled)
            Settings.System.putString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_7, value)
        } else if (preference === mLeftSwipeApp) {
            val value = newValue as String
            val gestureDisabled = value == AppSelectListPreference.Companion.DISABLED_ENTRY
            setGestureEnabled(KEY_LEFT_SWIPE_APP, !gestureDisabled)
            Settings.System.putString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_8, value)
        } else if (preference === mRightSwipeApp) {
            val value = newValue as String
            val gestureDisabled = value == AppSelectListPreference.Companion.DISABLED_ENTRY
            setGestureEnabled(KEY_RIGHT_SWIPE_APP, !gestureDisabled)
            Settings.System.putString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_9, value)
        } else if (preference === mSGestureApp) {
            val value = newValue as String
            val gestureDisabled = value == AppSelectListPreference.Companion.DISABLED_ENTRY
            setGestureEnabled(KEY_S_GESTURE_APP, !gestureDisabled)
            Settings.System.putString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_10, value)
        } else if (preference === mWGestureApp) {
            val value = newValue as String
            val gestureDisabled = value == AppSelectListPreference.Companion.DISABLED_ENTRY
            setGestureEnabled(KEY_W_GESTURE_APP, !gestureDisabled)
            Settings.System.putString(getContext().getContentResolver(), DEVICE_GESTURE_MAPPING_11, value)
        }
        return true
    }

    private fun setMusicPlaybackGestureEnabled(enabled: Boolean) {
        val playbackSupported = isGestureSupported(KEY_MUSIC_START)
        val nextTrackSupported = isGestureSupported(KEY_MUSIC_TRACK_NEXT)
        val prevTrackSupported = isGestureSupported(KEY_MUSIC_TRACK_PREV)
        if (playbackSupported && nextTrackSupported && prevTrackSupported) {
            setGestureEnabled(KEY_MUSIC_START, if (enabled) playbackSupported else false)
            setGestureEnabled(KEY_MUSIC_TRACK_NEXT, if (enabled) nextTrackSupported else false)
            setGestureEnabled(KEY_MUSIC_TRACK_PREV, if (enabled) prevTrackSupported else false)
        } else {
            Log.e(TAG, "Cannot write to music playback gesture files")
        }
    }

    private fun isGestureSupported(key: String): Boolean {
        return Utils.fileWritable(getGestureFile(key))
    }

    private fun setGestureEnabled(key: String, enabled: Boolean) {
        Utils.writeValue(getGestureFile(key), if (enabled) "1" else "0")
    }

    fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference !is AppSelectListPreference) {
            super.onDisplayPreferenceDialog(preference)
            return
        }
        val fragment: DialogFragment? = AppSelectListPreferenceDialogFragment.newInstance(preference.getKey())
        fragment.setTargetFragment(this, 0)
        fragment.show(getFragmentManager(), "dialog_preference")
    }

    fun onResume() {
        super.onResume()
        mFPDownSwipeApp?.setEnabled(!areSystemNavigationKeysEnabled())
        mFPUpSwipeApp?.setEnabled(!areSystemNavigationKeysEnabled())
    }

    private fun loadInstalledPackages() {
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val installedAppsInfo: List<ResolveInfo> = mPm.queryIntentActivities(mainIntent, 0)
        for (info in installedAppsInfo) {
            val activity: ActivityInfo = info.activityInfo
            val appInfo: ApplicationInfo = activity.applicationInfo
            val componentName = ComponentName(appInfo.packageName, activity.name)
            var label: CharSequence? = null
            try {
                label = activity.loadLabel(mPm)
            } catch (e: Exception) {
            }
            if (label != null) {
                val item = PackageItem(activity.loadLabel(mPm), 0, componentName)
                mInstalledPackages.add(item)
            }
        }
        Collections.sort(mInstalledPackages)
    }

    private inner class FetchPackageInformationTask : AsyncTask<Void?, Void?, Void?>() {
        protected fun doInBackground(vararg params: Void?): Void? {
            loadInstalledPackages()
            return null
        }

        protected fun onPostExecute(feed: Void?) {
            mCircleApp!!.setPackageList(mInstalledPackages)
            mDownArrowApp!!.setPackageList(mInstalledPackages)
            mMGestureApp!!.setPackageList(mInstalledPackages)
            mDownSwipeApp!!.setPackageList(mInstalledPackages)
            mUpSwipeApp!!.setPackageList(mInstalledPackages)
            mLeftSwipeApp!!.setPackageList(mInstalledPackages)
            mRightSwipeApp!!.setPackageList(mInstalledPackages)
            mSGestureApp!!.setPackageList(mInstalledPackages)
            mWGestureApp!!.setPackageList(mInstalledPackages)
        }
    }

    companion object {
        const val TAG = "GestureSettings"
        const val KEY_PROXI_SWITCH = "proxi"
        const val KEY_OFF_SCREEN_GESTURE_FEEDBACK_SWITCH = "off_screen_gesture_feedback"
        const val KEY_MUSIC_START = "music_playback_gesture"
        const val KEY_CIRCLE_APP = "circle_gesture_app"
        const val KEY_DOWN_ARROW_APP = "down_arrow_gesture_app"
        const val KEY_MUSIC_TRACK_PREV = "left_arrow_gesture_app"
        const val KEY_MUSIC_TRACK_NEXT = "right_arrow_gesture_app"
        const val KEY_UP_ARROW_APP = "down_up_gesture_app"
        const val KEY_DOWN_SWIPE_APP = "down_swipe_gesture_app"
        const val KEY_UP_SWIPE_APP = "up_swipe_gesture_app"
        const val KEY_LEFT_SWIPE_APP = "left_swipe_gesture_app"
        const val KEY_RIGHT_SWIPE_APP = "right_swipe_gesture_app"
        const val KEY_M_GESTURE_APP = "gesture_m_app"
        const val KEY_S_GESTURE_APP = "gesture_s_app"
        const val KEY_W_GESTURE_APP = "gesture_w_app"
        const val KEY_FP_GESTURE_CATEGORY = "key_fp_gesture_category"
        const val KEY_FP_GESTURE_DEFAULT_CATEGORY = "gesture_settings"
        const val DEVICE_GESTURE_MAPPING_0 = "device_gesture_mapping_0_0"
        const val DEVICE_GESTURE_MAPPING_1 = "device_gesture_mapping_1_0"
        const val DEVICE_GESTURE_MAPPING_2 = "device_gesture_mapping_2_0"
        const val DEVICE_GESTURE_MAPPING_3 = "device_gesture_mapping_3_0"
        const val DEVICE_GESTURE_MAPPING_4 = "device_gesture_mapping_4_0"
        const val DEVICE_GESTURE_MAPPING_5 = "device_gesture_mapping_5_0"
        const val DEVICE_GESTURE_MAPPING_6 = "device_gesture_mapping_6_0"
        const val DEVICE_GESTURE_MAPPING_7 = "device_gesture_mapping_7_0"
        const val DEVICE_GESTURE_MAPPING_8 = "device_gesture_mapping_8_0"
        const val DEVICE_GESTURE_MAPPING_9 = "device_gesture_mapping_9_0"
        const val DEVICE_GESTURE_MAPPING_10 = "device_gesture_mapping_10_0"
        const val DEVICE_GESTURE_MAPPING_11 = "device_gesture_mapping_11_0"
        const val DEVICE_GESTURE_MAPPING_12 = "device_gesture_mapping_12_0"
        const val DEVICE_GESTURE_MAPPING_13 = "device_gesture_mapping_13_0"
        const val DEVICE_GESTURE_MAPPING_14 = "device_gesture_mapping_14_0"
        fun getGestureFile(key: String?): String? {
            when (key) {
                KEY_CIRCLE_APP -> return "/proc/touchpanel/letter_o_enable" //getContext().getResources().getString(R.string.pathGestureCircle); //"/proc/touchpanel/letter_o_enable";
                KEY_MUSIC_START -> return "/proc/touchpanel/double_swipe_enable" //getContext().getResources().getString(R.string.pathGestureDoubleSwipe); //"/proc/touchpanel/double_swipe_enable";
                KEY_DOWN_ARROW_APP -> return "/proc/touchpanel/down_arrow_enable" //getContext().getResources().getString(R.string.pathGestureDownArrow); //"/proc/touchpanel/down_arrow_enable";
                KEY_MUSIC_TRACK_PREV -> return "/proc/touchpanel/left_arrow_enable" //getContext().getResources().getString(R.string.pathGestureLeftArrow); //"/proc/touchpanel/left_arrow_enable";
                KEY_MUSIC_TRACK_NEXT -> return "/proc/touchpanel/right_arrow_enable" //getContext().getResources().getString(R.string.pathGestureRightArrow); //"/proc/touchpanel/right_arrow_enable";
                KEY_DOWN_SWIPE_APP -> return "/proc/touchpanel/down_swipe_enable" //getContext().getResources().getString(R.string.pathGestureDownSwipe); //"/proc/touchpanel/down_swipe_enable";
                KEY_UP_SWIPE_APP -> return "/proc/touchpanel/up_swipe_enable" //getContext().getResources().getString(R.string.pathGestureUpSwipe); //"/proc/touchpanel/up_swipe_enable";
                KEY_LEFT_SWIPE_APP -> return "/proc/touchpanel/left_swipe_enable" //getContext().getResources().getString(R.string.pathGestureLeftSwipe); //"/proc/touchpanel/left_swipe_enable";
                KEY_RIGHT_SWIPE_APP -> return "/proc/touchpanel/right_swipe_enable" //getContext().getResources().getString(R.string.pathGestureRightSwipe); //"/proc/touchpanel/right_swipe_enable";
                KEY_M_GESTURE_APP -> return "/proc/touchpanel/letter_m_enable" //getContext().getResources().getString(R.string.pathGestureM); //"/proc/touchpanel/letter_m_enable";
                KEY_S_GESTURE_APP -> return "/proc/touchpanel/letter_s_enable" //getContext().getResources().getString(R.string.pathGestureS); //"/proc/touchpanel/letter_s_enable";
                KEY_W_GESTURE_APP -> return "/proc/touchpanel/letter_w_enable" //getContext().getResources().getString(R.string.pathGestureW); //"/proc/touchpanel/letter_w_enable";
            }
            return null
        }
    }
}