/*
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

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.AttributeSet
import com.aicp.device.Utils.fileWritable
import com.aicp.device.Utils.getFileValue
import com.aicp.device.Utils.writeValue

class VibratorNotifStrengthPreference(context: Context, attrs: AttributeSet?) : VibratorStrengthPreference(context, attrs) {
    private val mMinValue = 116
    private val mMaxValue = 2088
    private val mVibrator: Vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    override fun getValue(context: Context?): String {
        return getFileValue(FILE_LEVEL, DEFAULT_VALUE)
    }

    override fun setValue(newValue: String?, withFeedback: Boolean) {
        writeValue(FILE_LEVEL, newValue)
        Settings.System.putString(context.contentResolver, SETTINGS_KEY, newValue)
        if (withFeedback) {
            mVibrator.vibrate(testVibrationPattern)
        }
    }

    companion object {
        protected var FILE_LEVEL = "/sys/class/leds/vibrator/vmax_mv_strong"
        protected var testVibrationPattern: VibrationEffect = VibrationEffect.createOneShot(250,
            VibrationEffect.DEFAULT_AMPLITUDE
        )
        protected var SETTINGS_KEY = DeviceSettings.KEY_SETTINGS_PREFIX + DeviceSettings.KEY_NOTIF_VIBSTRENGTH
        protected var DEFAULT_VALUE = "2008"
        val isSupported: Boolean
            get() = fileWritable(FILE_LEVEL)

        @JvmStatic
        fun restore(context: Context) {
            if (!isSupported) {
                return
            }
            var storedValue: String = Settings.System.getString(context.contentResolver, SETTINGS_KEY)
            writeValue(FILE_LEVEL, storedValue)
        }
    }

    init {
        // from drivers/platform/msm/qpnp-haptic.c
        // #define QPNP_HAP_VMAX_MIN_MV		116
        // #define QPNP_HAP_VMAX_MAX_MV		3596
        layoutResource = R.layout.preference_seek_bar
    }
}
