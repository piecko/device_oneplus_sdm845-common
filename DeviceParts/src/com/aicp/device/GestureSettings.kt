/*
* Copyright (C) 2017 The OmniROM Project
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

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.DialogFragment
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference
import com.aicp.device.AppSelectListPreference.PackageItem
import com.aicp.device.Utils.fileWritable
import com.aicp.device.Utils.writeValue
import com.android.settingslib.CustomDialogPreferenceCompat
import java.util.*

class GestureSettings : PreferenceFragmentCompat(),
    Preference.OnPreferenceChangeListener {
    private var mProxiSwitch: TwoStatePreference? = null
    private val mFpSwipeDownSwitch: TwoStatePreference? = null
    private var mOffscreenGestureFeedbackSwitch: TwoStatePreference? = null
    private var mDoubleSwipeApp: AppSelectListPreference? = null
    private var mCircleApp: AppSelectListPreference? = null
    private var mDownArrowApp: AppSelectListPreference? = null
    private var mUpArrowApp: AppSelectListPreference? = null
    private var mLeftArrowApp: AppSelectListPreference? = null
    private var mRightArrowApp: AppSelectListPreference? = null
    private var mDownSwipeApp: AppSelectListPreference? = null
    private var mUpSwipeApp: AppSelectListPreference? = null
    private var mLeftSwipeApp: AppSelectListPreference? = null
    private var mRightSwipeApp: AppSelectListPreference? = null
    private val mFPDownSwipeApp: AppSelectListPreference? = null
    private val mFPUpSwipeApp: AppSelectListPreference? = null
    private val mFPRightSwipeApp: AppSelectListPreference? = null
    private val mFPLeftSwipeApp: AppSelectListPreference? = null
    private val fpGestures: PreferenceCategory? = null
    private val mInstalledPackages: MutableList<PackageItem> =
        LinkedList()
    private var mPm: PackageManager? = null
    override fun onCreatePreferences(
        savedInstanceState: Bundle,
        rootKey: String
    ) {
        setPreferencesFromResource(R.xml.gesture_settings, rootKey)
        mPm = context?.packageManager
        mProxiSwitch =
            findPreference<Preference>(KEY_PROXI_SWITCH) as TwoStatePreference?
        mProxiSwitch!!.isChecked = Settings.System.getInt(
            context?.contentResolver,
            Settings.System.OMNI_DEVICE_PROXI_CHECK_ENABLED, 1
        ) != 0
        mOffscreenGestureFeedbackSwitch =
            findPreference<Preference>(KEY_OFF_SCREEN_GESTURE_FEEDBACK_SWITCH) as TwoStatePreference?
        mOffscreenGestureFeedbackSwitch!!.isChecked = Settings.System.getInt(
            context?.contentResolver,
            "Settings.System." + KeyHandler.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
            1
        ) != 0
        mDoubleSwipeApp =
            findPreference<Preference>(KEY_DOUBLE_SWIPE_APP) as AppSelectListPreference?
        mDoubleSwipeApp!!.isEnabled = isGestureSupported(KEY_DOUBLE_SWIPE_APP)
        var value = Settings.System.getString(
            context?.contentResolver,
            DEVICE_GESTURE_MAPPING_0
        )
        mDoubleSwipeApp!!.value = value
        mDoubleSwipeApp!!.onPreferenceChangeListener = this
        mCircleApp =
            findPreference<Preference>(KEY_CIRCLE_APP) as AppSelectListPreference?
        mCircleApp!!.isEnabled = isGestureSupported(KEY_CIRCLE_APP)
        value = Settings.System.getString(
            context?.contentResolver,
            DEVICE_GESTURE_MAPPING_1
        )
        mCircleApp!!.value = value
        mCircleApp!!.onPreferenceChangeListener = this
        mDownArrowApp =
            findPreference<Preference>(KEY_DOWN_ARROW_APP) as AppSelectListPreference?
        mDownArrowApp!!.isEnabled = isGestureSupported(KEY_DOWN_ARROW_APP)
        value = Settings.System.getString(
            context?.contentResolver,
            DEVICE_GESTURE_MAPPING_2
        )
        mDownArrowApp!!.value = value
        mDownArrowApp!!.onPreferenceChangeListener = this
        mUpArrowApp =
            findPreference<Preference>(KEY_UP_ARROW_APP) as AppSelectListPreference?
        mUpArrowApp!!.isEnabled = isGestureSupported(KEY_UP_ARROW_APP)
        value = Settings.System.getString(
            context?.contentResolver,
            DEVICE_GESTURE_MAPPING_3
        )
        mUpArrowApp!!.value = value
        mUpArrowApp!!.onPreferenceChangeListener = this
        mLeftArrowApp =
            findPreference<Preference>(KEY_LEFT_ARROW_APP) as AppSelectListPreference?
        mLeftArrowApp!!.isEnabled = isGestureSupported(KEY_LEFT_ARROW_APP)
        value = Settings.System.getString(
            context?.contentResolver,
            DEVICE_GESTURE_MAPPING_4
        )
        mLeftArrowApp!!.value = value
        mLeftArrowApp!!.onPreferenceChangeListener = this
        mRightArrowApp =
            findPreference<Preference>(KEY_RIGHT_ARROW_APP) as AppSelectListPreference?
        mRightArrowApp!!.isEnabled = isGestureSupported(KEY_RIGHT_ARROW_APP)
        value = Settings.System.getString(
            context?.contentResolver,
            DEVICE_GESTURE_MAPPING_5
        )
        mRightArrowApp!!.value = value
        mRightArrowApp!!.onPreferenceChangeListener = this
        mDownSwipeApp =
            findPreference<Preference>(KEY_DOWN_SWIPE_APP) as AppSelectListPreference?
        mDownSwipeApp!!.isEnabled = isGestureSupported(KEY_DOWN_SWIPE_APP)
        value = Settings.System.getString(
            context?.contentResolver,
            DEVICE_GESTURE_MAPPING_6
        )
        mDownSwipeApp!!.value = value
        mDownSwipeApp!!.onPreferenceChangeListener = this
        mUpSwipeApp =
            findPreference<Preference>(KEY_UP_SWIPE_APP) as AppSelectListPreference?
        mUpSwipeApp!!.isEnabled = isGestureSupported(KEY_UP_SWIPE_APP)
        value = Settings.System.getString(
            context?.contentResolver,
            DEVICE_GESTURE_MAPPING_7
        )
        mUpSwipeApp!!.value = value
        mUpSwipeApp!!.onPreferenceChangeListener = this
        mLeftSwipeApp =
            findPreference<Preference>(KEY_LEFT_SWIPE_APP) as AppSelectListPreference?
        mLeftSwipeApp!!.isEnabled = isGestureSupported(KEY_LEFT_SWIPE_APP)
        value = Settings.System.getString(
            context?.contentResolver,
            DEVICE_GESTURE_MAPPING_8
        )
        mLeftSwipeApp!!.value = value
        mLeftSwipeApp!!.onPreferenceChangeListener = this
        mRightSwipeApp =
            findPreference<Preference>(KEY_RIGHT_SWIPE_APP) as AppSelectListPreference?
        mRightSwipeApp!!.isEnabled = isGestureSupported(KEY_RIGHT_SWIPE_APP)
        value = Settings.System.getString(
            context?.contentResolver,
            DEVICE_GESTURE_MAPPING_9
        )
        mRightSwipeApp!!.value = value
        mRightSwipeApp!!.onPreferenceChangeListener = this
        FetchPackageInformationTask().execute()
    }

    private fun areSystemNavigationKeysEnabled(): Boolean {
        return Settings.Secure.getInt(
            context?.contentResolver,
            Settings.Secure.SYSTEM_NAVIGATION_KEYS_ENABLED, 0
        ) == 1
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference === mProxiSwitch) {
            Settings.System.putInt(
                context?.contentResolver,
                Settings.System.OMNI_DEVICE_PROXI_CHECK_ENABLED,
                if (mProxiSwitch!!.isChecked) 1 else 0
            )
            return true
        }
        if (preference === mOffscreenGestureFeedbackSwitch) {
            Settings.System.putInt(
                context?.contentResolver,
                "Settings.System." + KeyHandler.GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                if (mOffscreenGestureFeedbackSwitch!!.isChecked) 1 else 0
            )
            return true
        }
        return super.onPreferenceTreeClick(preference)
    }

    override fun onPreferenceChange(
        preference: Preference,
        newValue: Any
    ): Boolean {
        when {
            preference === mDoubleSwipeApp -> {
                val value = newValue as String
                val gestureDisabled = value == AppSelectListPreference.DISABLED_ENTRY
                setGestureEnabled(
                    KEY_DOUBLE_SWIPE_APP,
                    !gestureDisabled
                )
                Settings.System.putString(
                    context?.contentResolver,
                    DEVICE_GESTURE_MAPPING_0,
                    value
                )
            }
            preference === mCircleApp -> {
                val value = newValue as String
                val gestureDisabled = value == AppSelectListPreference.DISABLED_ENTRY
                setGestureEnabled(
                    KEY_CIRCLE_APP,
                    !gestureDisabled
                )
                Settings.System.putString(
                    context?.contentResolver,
                    DEVICE_GESTURE_MAPPING_1,
                    value
                )
            }
            preference === mDownArrowApp -> {
                val value = newValue as String
                val gestureDisabled = value == AppSelectListPreference.DISABLED_ENTRY
                setGestureEnabled(
                    KEY_DOWN_ARROW_APP,
                    !gestureDisabled
                )
                Settings.System.putString(
                    context?.contentResolver,
                    DEVICE_GESTURE_MAPPING_2,
                    value
                )
            }
            preference === mUpArrowApp -> {
                val value = newValue as String
                val gestureDisabled = value == AppSelectListPreference.DISABLED_ENTRY
                setGestureEnabled(
                    KEY_UP_ARROW_APP,
                    !gestureDisabled
                )
                Settings.System.putString(
                    context?.contentResolver,
                    DEVICE_GESTURE_MAPPING_3,
                    value
                )
            }
            preference === mLeftArrowApp -> {
                val value = newValue as String
                val gestureDisabled = value == AppSelectListPreference.DISABLED_ENTRY
                setGestureEnabled(
                    KEY_LEFT_ARROW_APP,
                    !gestureDisabled
                )
                Settings.System.putString(
                    context?.contentResolver,
                    DEVICE_GESTURE_MAPPING_4,
                    value
                )
            }
            preference === mRightArrowApp -> {
                val value = newValue as String
                val gestureDisabled = value == AppSelectListPreference.DISABLED_ENTRY
                setGestureEnabled(
                    KEY_RIGHT_ARROW_APP,
                    !gestureDisabled
                )
                Settings.System.putString(
                    context?.contentResolver,
                    DEVICE_GESTURE_MAPPING_5,
                    value
                )
            }
            preference === mDownSwipeApp -> {
                val value = newValue as String
                val gestureDisabled = value == AppSelectListPreference.DISABLED_ENTRY
                setGestureEnabled(
                    KEY_DOWN_SWIPE_APP,
                    !gestureDisabled
                )
                Settings.System.putString(
                    context?.contentResolver,
                    DEVICE_GESTURE_MAPPING_6,
                    value
                )
            }
            preference === mUpSwipeApp -> {
                val value = newValue as String
                val gestureDisabled = value == AppSelectListPreference.DISABLED_ENTRY
                setGestureEnabled(
                    KEY_UP_SWIPE_APP,
                    !gestureDisabled
                )
                Settings.System.putString(
                    context?.contentResolver,
                    DEVICE_GESTURE_MAPPING_7,
                    value
                )
            }
            preference === mLeftSwipeApp -> {
                val value = newValue as String
                val gestureDisabled = value == AppSelectListPreference.DISABLED_ENTRY
                setGestureEnabled(
                    KEY_LEFT_SWIPE_APP,
                    !gestureDisabled
                )
                Settings.System.putString(
                    context?.contentResolver,
                    DEVICE_GESTURE_MAPPING_8,
                    value
                )
            }
            preference === mRightSwipeApp -> {
                val value = newValue as String
                val gestureDisabled = value == AppSelectListPreference.DISABLED_ENTRY
                setGestureEnabled(
                    KEY_RIGHT_SWIPE_APP,
                    !gestureDisabled
                )
                Settings.System.putString(
                    context?.contentResolver,
                    DEVICE_GESTURE_MAPPING_9,
                    value
                )
            }
        }
        return true
    }

    private fun isGestureSupported(key: String): Boolean {
        return fileWritable(
            getGestureFile(
                key
            )
        )
    }

    private fun setGestureEnabled(key: String, enabled: Boolean) {
        writeValue(
            getGestureFile(
                key
            ), if (enabled) "1" else "0"
        )
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference !is AppSelectListPreference) {
            super.onDisplayPreferenceDialog(preference)
            return
        }
        val fragment: DialogFragment =
            CustomDialogPreferenceCompat.CustomPreferenceDialogFragment
                .newInstance(preference.getKey())
        fragment.setTargetFragment(this, 0)
        parentFragmentManager.let { fragment.show(it, "dialog_preference") }
    }

    override fun onResume() {
        super.onResume()
        if (mFPDownSwipeApp != null) {
            mFPDownSwipeApp.isEnabled = !areSystemNavigationKeysEnabled()
        }
        if (mFPUpSwipeApp != null) {
            mFPUpSwipeApp.isEnabled = !areSystemNavigationKeysEnabled()
        }
    }

    private fun loadInstalledPackages() {
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val installedAppsInfo =
            mPm!!.queryIntentActivities(mainIntent, 0)
        for (info in installedAppsInfo) {
            val activity = info.activityInfo
            val appInfo = activity.applicationInfo
            val componentName = ComponentName(appInfo.packageName, activity.name)
            var label: CharSequence? = null
            try {
                label = activity.loadLabel(mPm!!)
            } catch (e: Exception) {
            }
            if (label != null) {
                val item = PackageItem(activity.loadLabel(mPm!!), 0, componentName)
                mInstalledPackages.add(item)
            }
        }
        mInstalledPackages.sort()
    }

    private inner class FetchPackageInformationTask :
        AsyncTask<Void?, Void?, Void?>() {
        override fun doInBackground(vararg p0: Void?): Void? {
            loadInstalledPackages()
            return null
        }

        override fun onPostExecute(feed: Void?) {
            mDoubleSwipeApp!!.setPackageList(mInstalledPackages)
            mCircleApp!!.setPackageList(mInstalledPackages)
            mDownArrowApp!!.setPackageList(mInstalledPackages)
            mUpArrowApp!!.setPackageList(mInstalledPackages)
            mLeftArrowApp!!.setPackageList(mInstalledPackages)
            mRightArrowApp!!.setPackageList(mInstalledPackages)
            mDownSwipeApp!!.setPackageList(mInstalledPackages)
            mUpSwipeApp!!.setPackageList(mInstalledPackages)
            mLeftSwipeApp!!.setPackageList(mInstalledPackages)
            mRightSwipeApp!!.setPackageList(mInstalledPackages)
        }
    }

    companion object {
        const val KEY_PROXI_SWITCH = "proxi"
        const val KEY_OFF_SCREEN_GESTURE_FEEDBACK_SWITCH = "off_screen_gesture_feedback"
        const val KEY_DOUBLE_SWIPE_APP = "double_swipe_gesture_app"
        const val KEY_CIRCLE_APP = "circle_gesture_app"
        const val KEY_DOWN_ARROW_APP = "down_arrow_gesture_app"
        const val KEY_UP_ARROW_APP = "up_arrow_gesture_app"
        const val KEY_LEFT_ARROW_APP = "left_arrow_gesture_app"
        const val KEY_RIGHT_ARROW_APP = "right_arrow_gesture_app"
        const val KEY_DOWN_SWIPE_APP = "down_swipe_gesture_app"
        const val KEY_UP_SWIPE_APP = "up_swipe_gesture_app"
        const val KEY_LEFT_SWIPE_APP = "left_swipe_gesture_app"
        const val KEY_RIGHT_SWIPE_APP = "right_swipe_gesture_app"
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
        fun getGestureFile(key: String?): String? {
            when (key) {
                KEY_CIRCLE_APP -> return "/proc/touchpanel/letter_o_enable"
                KEY_DOUBLE_SWIPE_APP -> return "/proc/touchpanel/double_swipe_enable"
                KEY_DOWN_ARROW_APP -> return "/proc/touchpanel/down_arrow_enable"
                KEY_UP_ARROW_APP -> return "/proc/touchpanel/up_arrow_enable"
                KEY_LEFT_ARROW_APP -> return "/proc/touchpanel/left_arrow_enable"
                KEY_RIGHT_ARROW_APP -> return "/proc/touchpanel/right_arrow_enable"
                KEY_DOWN_SWIPE_APP -> return "/proc/touchpanel/down_swipe_enable"
                KEY_UP_SWIPE_APP -> return "/proc/touchpanel/up_swipe_enable"
                KEY_LEFT_SWIPE_APP -> return "/proc/touchpanel/left_swipe_enable"
                KEY_RIGHT_SWIPE_APP -> return "/proc/touchpanel/right_swipe_enable"
            }
            return null
        }
    }
}