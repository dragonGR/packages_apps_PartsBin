/*
 * Copyright (C) 2015 The CyanogenMod Project
 * Copyright (C) 2017 The LineageOS Project
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
import android.content.Context
import android.os.UserHandle
import android.provider.Settings

object Constants {
    // Preference keys
    const val NOTIF_SLIDER_TOP_KEY = "keycode_top_position"
    const val NOTIF_SLIDER_MIDDLE_KEY = "keycode_middle_position"
    const val NOTIF_SLIDER_BOTTOM_KEY = "keycode_bottom_position"

    // Button prefs
    const val NOTIF_SLIDER_TOP_PREF = "pref_keycode_top_position"
    const val NOTIF_SLIDER_MIDDLE_PREF = "pref_keycode_middle_position"
    const val NOTIF_SLIDER_BOTTOM_PREF = "pref_keycode_bottom_position"

    // Default values
    const val KEY_VALUE_TOTAL_SILENCE = 0
    const val KEY_VALUE_SILENT = 1
    const val KEY_VALUE_PRIORTY_ONLY = 2
    const val KEY_VALUE_VIBRATE = 3
    const val KEY_VALUE_NORMAL = 4

    // Single tap key code
    const val KEY_SINGLE_TAP = 255

    // Key Codes
    const val KEY_DOUBLE_TAP = 143
    const val KEY_HOME = 102
    const val KEY_BACK = 158
    const val KEY_RECENTS = 580
    const val KEY_SLIDER_TOP = 601
    const val KEY_SLIDER_CENTER = 602
    const val KEY_SLIDER_BOTTOM = 603

    // TriStateUI Modes
    const val MODE_TOTAL_SILENCE = 600
    const val MODE_ALARMS_ONLY = 601
    const val MODE_PRIORITY_ONLY = 602
    const val MODE_NONE = 603
    const val MODE_VIBRATE = 604
    const val MODE_RING = 605

    // AICP additions: arbitrary value which hopefully doesn't conflict with upstream anytime soon
    const val MODE_SILENT = 620
    const val MODE_FLASHLIGHT = 621

    // Gesture constants
    const val GESTURE_W_SCANCODE = 246
    const val GESTURE_M_SCANCODE = 247
    const val GESTURE_S_SCANCODE = 248
    const val GESTURE_CIRCLE_SCANCODE = 250
    const val GESTURE_II_SCANCODE = 251
    const val GESTURE_V_SCANCODE = 252
    const val GESTURE_LEFT_V_SCANCODE = 253
    const val GESTURE_RIGHT_V_SCANCODE = 254
    const val DOZE_INTENT = "com.android.systemui.doze.pulse"
    const val ACTION_UPDATE_SLIDER_POSITION = "com.aicp.device.UPDATE_SLIDER_POSITION"
    const val EXTRA_SLIDER_POSITION = "position"
    const val EXTRA_SLIDER_POSITION_VALUE = "position_value"
    const val EXTRA_SLIDER_DEFAULT_VALUE = "2,1,0"
    const val GESTURE_HAPTIC_DURATION = 50
    const val GESTURE_WAKELOCK_DURATION = 2000
    val sHandledGestures = intArrayOf(
            KEY_SINGLE_TAP,
            KEY_SLIDER_TOP,
            KEY_SLIDER_CENTER,
            KEY_SLIDER_BOTTOM
    )
    val sSupportedGestures = intArrayOf(
            GESTURE_II_SCANCODE,
            GESTURE_CIRCLE_SCANCODE,
            GESTURE_V_SCANCODE,
            GESTURE_LEFT_V_SCANCODE,
            GESTURE_RIGHT_V_SCANCODE,
            GESTURE_M_SCANCODE,
            GESTURE_W_SCANCODE,
            GESTURE_S_SCANCODE,
            KEY_SINGLE_TAP,
            KEY_DOUBLE_TAP,
            KEY_SLIDER_TOP,
            KEY_SLIDER_CENTER,
            KEY_SLIDER_BOTTOM
    )
}