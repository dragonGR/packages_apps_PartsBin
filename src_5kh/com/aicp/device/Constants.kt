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

object Constants {
    // Default values
    const val KEY_VALUE_TOTAL_SILENCE = 0
    const val KEY_VALUE_SILENT = 1
    const val KEY_VALUE_PRIORTY_ONLY = 2
    const val KEY_VALUE_VIBRATE = 3
    const val KEY_VALUE_NORMAL = 4

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
    const val KEY_DOUBLE_TAP = 143
    const val KEY_HOME = 102
    const val KEY_BACK = 158
    const val KEY_RECENTS = 580
    const val KEY_SLIDER_TOP = 601
    const val KEY_SLIDER_CENTER = 602
    const val KEY_SLIDER_BOTTOM = 603

    // Gesture constants
    const val GESTURE_RIGHT_SWIPE_SCANCODE = 63
    const val GESTURE_LEFT_SWIPE_SCANCODE = 64
    const val GESTURE_DOWN_SWIPE_SCANCODE = 65
    const val GESTURE_UP_SWIPE_SCANCODE = 66
    const val GESTURE_W_SCANCODE = 246
    const val GESTURE_M_SCANCODE = 247
    const val GESTURE_S_SCANCODE = 248
    const val GESTURE_CIRCLE_SCANCODE = 250
    const val GESTURE_II_SCANCODE = 251
    const val GESTURE_V_SCANCODE = 252
    const val GESTURE_LEFT_V_SCANCODE = 253
    const val GESTURE_RIGHT_V_SCANCODE = 254

    // FP Gesture constants
    const val FP_GESTURE_SWIPE_DOWN = 108
    const val FP_GESTURE_SWIPE_UP = 103
    const val FP_GESTURE_SWIPE_LEFT = 105
    const val FP_GESTURE_SWIPE_RIGHT = 106
    const val FP_GESTURE_LONG_PRESS = 305
    const val MIN_PULSE_INTERVAL_MS = 2500
    const val DOZE_INTENT = "com.android.systemui.doze.pulse"
    const val HANDWAVE_MAX_DELTA_MS = 1000
    const val POCKET_MIN_DELTA_MS = 5000
    const val GESTURE_REQUEST = 1
    const val GESTURE_WAKELOCK_DURATION = 2000
    const val GESTURE_HAPTIC_DURATION = 50
    const val ACTION_UPDATE_SLIDER_POSITION = "com.aicp.device.UPDATE_SLIDER_POSITION"
    const val EXTRA_SLIDER_POSITION = "position"
    const val EXTRA_SLIDER_POSITION_VALUE = "position_value"
    val sSupportedGestures5t = intArrayOf(
            GESTURE_II_SCANCODE,
            GESTURE_CIRCLE_SCANCODE,
            GESTURE_V_SCANCODE,
            GESTURE_LEFT_V_SCANCODE,
            GESTURE_RIGHT_V_SCANCODE,
            GESTURE_DOWN_SWIPE_SCANCODE,
            GESTURE_UP_SWIPE_SCANCODE,
            GESTURE_LEFT_SWIPE_SCANCODE,
            GESTURE_RIGHT_SWIPE_SCANCODE,
            GESTURE_M_SCANCODE,
            GESTURE_W_SCANCODE,
            GESTURE_S_SCANCODE,
            KEY_DOUBLE_TAP,
            KEY_SLIDER_TOP,
            KEY_SLIDER_CENTER,
            KEY_SLIDER_BOTTOM,
            FP_GESTURE_SWIPE_DOWN,
            FP_GESTURE_SWIPE_UP,
            FP_GESTURE_SWIPE_LEFT,
            FP_GESTURE_SWIPE_RIGHT,
            FP_GESTURE_LONG_PRESS)
    val sSupportedGestures = intArrayOf(
            GESTURE_II_SCANCODE,
            GESTURE_CIRCLE_SCANCODE,
            GESTURE_V_SCANCODE,
            GESTURE_LEFT_V_SCANCODE,
            GESTURE_RIGHT_V_SCANCODE,
            GESTURE_DOWN_SWIPE_SCANCODE,
            GESTURE_UP_SWIPE_SCANCODE,
            GESTURE_LEFT_SWIPE_SCANCODE,
            GESTURE_RIGHT_SWIPE_SCANCODE,
            GESTURE_M_SCANCODE,
            GESTURE_W_SCANCODE,
            GESTURE_S_SCANCODE,
            KEY_DOUBLE_TAP,
            KEY_SLIDER_TOP,
            KEY_SLIDER_CENTER,
            KEY_SLIDER_BOTTOM
    )
    val sHandledGestures = intArrayOf(
            KEY_SLIDER_TOP,
            KEY_SLIDER_CENTER,
            KEY_SLIDER_BOTTOM
    )
    val sProxiCheckedGestures = intArrayOf(
            GESTURE_II_SCANCODE,
            GESTURE_CIRCLE_SCANCODE,
            GESTURE_LEFT_V_SCANCODE,
            GESTURE_RIGHT_V_SCANCODE,
            GESTURE_DOWN_SWIPE_SCANCODE,
            GESTURE_UP_SWIPE_SCANCODE,
            GESTURE_LEFT_SWIPE_SCANCODE,
            GESTURE_RIGHT_SWIPE_SCANCODE,
            GESTURE_W_SCANCODE,
            GESTURE_M_SCANCODE,
            GESTURE_S_SCANCODE,
            GESTURE_V_SCANCODE,
            KEY_DOUBLE_TAP
    )
}