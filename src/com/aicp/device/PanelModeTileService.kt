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
import android.service.quicksettings.TileService

@TargetApi(24)
class PanelModeTileService : TileService() {
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
    }

    fun onStopListening() {
        super.onStopListening()
    }

    fun onClick() {
        super.onClick()
        val panelModes = Intent(this, PanelSettingsActivity::class.java)
        panelModes.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivityAndCollapse(panelModes)
    }
}