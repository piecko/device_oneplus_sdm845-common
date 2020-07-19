/*
* Copyright (C) 2016 The OmniROM Project
* Copyright (C) 2020 Android Ice Cold Project
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
import android.provider.Settings
import android.text.TextUtils
import androidx.preference.*

class DeviceSettings : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {
    private var mVibratorSystemStrength: VibratorSystemStrengthPreference? = null
    private var mVibratorCallStrength: VibratorCallStrengthPreference? = null
    private var mVibratorNotifStrength: VibratorNotifStrengthPreference? = null
    private var mSliderModeTop: ListPreference? = null
    private var mSliderModeCenter: ListPreference? = null
    private var mSliderModeBottom: ListPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main, rootKey)
        mVibratorSystemStrength = findPreference(KEY_SYSTEM_VIBSTRENGTH) as VibratorSystemStrengthPreference?
        if (mVibratorSystemStrength != null) {
            mVibratorSystemStrength?.isEnabled = VibratorSystemStrengthPreference.isSupported
        }
        mVibratorCallStrength = findPreference(KEY_CALL_VIBSTRENGTH) as VibratorCallStrengthPreference?
        if (mVibratorCallStrength != null) {
            mVibratorCallStrength?.isEnabled = VibratorCallStrengthPreference.isSupported
        }
        mVibratorNotifStrength = findPreference(KEY_NOTIF_VIBSTRENGTH) as VibratorNotifStrengthPreference?
        if (mVibratorNotifStrength != null) {
            mVibratorNotifStrength?.isEnabled = VibratorNotifStrengthPreference.isSupported
        }
        mSliderModeTop = findPreference(KEY_SLIDER_MODE_TOP) as ListPreference?
        mSliderModeTop!!.onPreferenceChangeListener = this
        val sliderModeTop = getSliderAction(0)
        var valueIndex: Int = mSliderModeTop!!.findIndexOfValue(sliderModeTop.toString())

        mSliderModeTop!!.setValueIndex(valueIndex)
        mSliderModeTop!!.summary = mSliderModeTop!!.entries[valueIndex]
        mSliderModeCenter = findPreference(KEY_SLIDER_MODE_CENTER) as ListPreference?
        mSliderModeCenter!!.onPreferenceChangeListener = this
        val sliderModeCenter = getSliderAction(1)
        valueIndex = mSliderModeCenter!!.findIndexOfValue(sliderModeCenter.toString())
        mSliderModeCenter!!.setValueIndex(valueIndex)
        mSliderModeCenter!!.summary = mSliderModeCenter!!.entries[valueIndex]
        mSliderModeBottom = findPreference(KEY_SLIDER_MODE_BOTTOM) as ListPreference?
        mSliderModeBottom!!.onPreferenceChangeListener = this
        val sliderModeBottom = getSliderAction(2)
        valueIndex = mSliderModeBottom!!.findIndexOfValue(sliderModeBottom.toString())
        mSliderModeBottom!!.setValueIndex(valueIndex)
        mSliderModeBottom!!.summary = mSliderModeBottom!!.entries[valueIndex]
        mHBMModeSwitch = findPreference(KEY_HBM_SWITCH) as TwoStatePreference?
        mHBMModeSwitch!!.isEnabled = HBMModeSwitch.isSupported
        mHBMModeSwitch!!.isChecked = HBMModeSwitch.isCurrentlyEnabled()
        mHBMModeSwitch!!.onPreferenceChangeListener = context?.let { HBMModeSwitch(it) }
        mDCDModeSwitch = findPreference(KEY_DCD_SWITCH) as TwoStatePreference?
        mDCDModeSwitch!!.isEnabled = DCDModeSwitch.isSupported
        mDCDModeSwitch!!.isChecked = DCDModeSwitch.isCurrentlyEnabled()
        mDCDModeSwitch!!.onPreferenceChangeListener = context?.let { DCDModeSwitch(it) }
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        when {
            preference === mSliderModeTop -> {
                val value = newValue as String?
                val sliderMode: Int = Integer.valueOf(value!!)
                setSliderAction(0, sliderMode)
                val valueIndex: Int = mSliderModeTop!!.findIndexOfValue(value)
                mSliderModeTop!!.summary = mSliderModeTop!!.entries[valueIndex]
            }
            preference === mSliderModeCenter -> {
                val value = newValue as String?
                val sliderMode: Int = Integer.valueOf(value!!)
                setSliderAction(1, sliderMode)
                val valueIndex: Int = mSliderModeCenter!!.findIndexOfValue(value)
                mSliderModeCenter!!.summary = mSliderModeCenter!!.entries[valueIndex]
            }
            preference === mSliderModeBottom -> {
                val value = newValue as String?
                val sliderMode: Int = Integer.valueOf(value!!)
                setSliderAction(2, sliderMode)
                val valueIndex: Int = mSliderModeBottom!!.findIndexOfValue(value)
                mSliderModeBottom!!.summary = mSliderModeBottom!!.entries[valueIndex]
            }
        }
        return true
    }

    private fun getSliderAction(position: Int): Int {
        var value: String? = Settings.System.getString(
            context?.contentResolver,
                Settings.System.OMNI_BUTTON_EXTRA_KEY_MAPPING)
        val defaultValue = SLIDER_DEFAULT_VALUE
        if (value == null) {
            value = defaultValue
        } else if (value.indexOf(",") == -1) {
            value = defaultValue
        }
        try {
            val parts: List<String> = value.split(",")
            return Integer.valueOf(parts[position])
        } catch (e: Exception) {
        }
        return 0
    }

    private fun setSliderAction(position: Int, action: Int) {
        var value: String? = Settings.System.getString(
            context?.contentResolver,
                Settings.System.OMNI_BUTTON_EXTRA_KEY_MAPPING)
        val defaultValue = SLIDER_DEFAULT_VALUE
        if (value == null) {
            value = defaultValue
        } else if (value.indexOf(",") == -1) {
            value = defaultValue
        }
        try {
            val parts: MutableList<String> = value.split(",") as MutableList<String>
            parts[position] += action.toString()
            val newValue: String = TextUtils.join(",", parts)
            Settings.System.putString(
                context?.contentResolver,
                    Settings.System.OMNI_BUTTON_EXTRA_KEY_MAPPING, newValue)
        } catch (e: Exception) {
        }
    }

    companion object {
        const val KEY_SYSTEM_VIBSTRENGTH = "vib_system_strength"
        const val KEY_CALL_VIBSTRENGTH = "vib_call_strength"
        const val KEY_NOTIF_VIBSTRENGTH = "vib_notif_strength"
        private const val KEY_SLIDER_MODE_TOP = "slider_mode_top"
        private const val KEY_SLIDER_MODE_CENTER = "slider_mode_center"
        private const val KEY_SLIDER_MODE_BOTTOM = "slider_mode_bottom"
        private const val KEY_CATEGORY_GRAPHICS = "graphics"
        const val KEY_SRGB_SWITCH = "srgb"
        const val KEY_HBM_SWITCH = "hbm"
        const val KEY_PROXI_SWITCH = "proxi"
        const val KEY_DCD_SWITCH = "dcd"
        const val KEY_DCI_SWITCH = "dci"
        const val KEY_WIDE_SWITCH = "wide"
        const val KEY_BACKLIGHT_DIMMER = "backlight_dimmer"
        const val KEY_HEADPHONE_GAIN = "headphone_gain"
        const val KEY_EARPIECE_GAIN = "earpiece_gain"
        const val KEY_MIC_GAIN = "mic_gain"
        const val KEY_SPEAKER_GAIN = "speaker_gain"
        const val SLIDER_DEFAULT_VALUE = "2,1,0"
        const val KEY_SETTINGS_PREFIX = "device_setting_"
        private var mHBMModeSwitch: TwoStatePreference? = null
        private var mDCDModeSwitch: TwoStatePreference? = null
    }
}
