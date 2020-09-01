/*
* Copyright (C) 2018 The OmniROM Project
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
import android.annotation.TargetApi
import android.content.Intent
import android.content.SharedPreferences
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.preference.PreferenceManager

@TargetApi(24)
class HBMModeTileService : TileService() {
    private var enabled = false
    private var mHBMOffState: String? = null
    private var mHBMOnState: String? = null
    fun onDestroy() {
        super.onDestroy()
    }

    fun onTileAdded() {
        super.onTileAdded()
    }

    fun onTileRemoved() {
        super.onTileRemoved()
    }

    fun onStartListening() {
        super.onStartListening()
        val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        enabled = HBMModeSwitch.Companion.isCurrentlyEnabled(this)
        mHBMOffState = this.getResources().getString(R.string.hbmOFF)
        mHBMOnState = this.getResources().getString(R.string.hbmON)
        getQsTile().setState(if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE)
        getQsTile().updateTile()
    }

    fun onStopListening() {
        super.onStopListening()
    }

    fun onClick() {
        super.onClick()
        val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        enabled = HBMModeSwitch.Companion.isCurrentlyEnabled(this)
        Utils.writeValue(HBMModeSwitch.Companion.getFile(this), if (enabled) mHBMOffState else mHBMOnState)
        sharedPrefs.edit().putBoolean(DeviceSettings.Companion.KEY_HBM_SWITCH, if (enabled) false else true).commit()
        //getQsTile().setLabel(enabled ? "HBM off" : "HBM On");
        getQsTile().setState(if (enabled) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE)
        getQsTile().updateTile()
    }
}