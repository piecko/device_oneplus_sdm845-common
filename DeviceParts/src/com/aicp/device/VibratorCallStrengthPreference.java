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
package com.aicp.device;

import android.content.ContentResolver;
import android.content.Context;
import android.util.AttributeSet;
import android.os.Vibrator;
import android.provider.Settings;

public class VibratorCallStrengthPreference extends VibratorStrengthPreference {

    private int mMinValue;
    private int mMaxValue;
    private Vibrator mVibrator;

    protected static String FILE_LEVEL = "/sys/class/leds/vibrator/vmax_mv_call";
    protected static long testVibrationPattern[] = {0,250};
    protected static String SETTINGS_KEY = DeviceSettings.KEY_SETTINGS_PREFIX + DeviceSettings.KEY_CALL_VIBSTRENGTH;
    protected static String DEFAULT_VALUE = "2008";

    public VibratorCallStrengthPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        // from drivers/platform/msm/qpnp-haptic.c
        // #define QPNP_HAP_VMAX_MIN_MV		116
        // #define QPNP_HAP_VMAX_MAX_MV		3596
        mMinValue = 116;
        mMaxValue = 2088;

        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        setLayoutResource(R.layout.preference_seek_bar);
    }

    @Override
    protected String getValue(Context context) {
        String val = Utils.getFileValue(FILE_LEVEL, DEFAULT_VALUE);
        return val;
    }

    @Override
    protected void setValue(String newValue, boolean withFeedback) {
        Utils.writeValue(FILE_LEVEL, newValue);
        Settings.System.putString(getContext().getContentResolver(), SETTINGS_KEY, newValue);
        if (withFeedback) {
            mVibrator.vibrate(testVibrationPattern, -1);
        }
    }

    public static boolean isSupported() {
        return Utils.fileWritable(FILE_LEVEL);
    }

    public static void restore(Context context) {
        if (!isSupported()) {
            return;
        }
        String storedValue = Settings.System.getString(context.getContentResolver(), SETTINGS_KEY);
        if (storedValue == null) {
            storedValue = DEFAULT_VALUE;
        }
        Utils.writeValue(FILE_LEVEL, storedValue);
    }

}

