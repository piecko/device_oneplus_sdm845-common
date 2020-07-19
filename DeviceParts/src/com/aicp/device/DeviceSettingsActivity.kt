/*
* Copyright (C) 2017 The OmniROM Project
* Copyright (C) 2020 ANdroid Ice Cold Project
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

import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

class DeviceSettingsActivity : FragmentActivity() {
    private var mDeviceSettingsFragment: DeviceSettings? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        mDeviceSettingsFragment = DeviceSettings()
        supportFragmentManager.beginTransaction()
                .add(android.R.id.content, mDeviceSettingsFragment!!)
                .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
