/*
* Copyright (C) 2017 The OmniROM Project
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

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.core.content.res.ResourcesCompat
import com.android.settingslib.CustomDialogPreferenceCompat
import java.util.*

class AppSelectListPreference : CustomDialogPreferenceCompat {
    private var mAdapter: AppSelectListAdapter? = null
    private var mAppIconDrawable: Drawable? = null
    private var mAppIconResourceId = 0
    private var mTitle: CharSequence? = null
    var value: String? = null
    private var mPm: PackageManager? = null
    private val mInstalledPackages: MutableList<PackageItem> = LinkedList()

    class PackageItem : Comparable<PackageItem> {
        val mTitle: CharSequence
        val mAppIconResourceId: Int
        val mComponentName: ComponentName?
        val mValue: String

        internal constructor(title: CharSequence, iconResourceId: Int, componentName: ComponentName) {
            mTitle = title
            mAppIconResourceId = iconResourceId
            mComponentName = componentName
            mValue = componentName.flattenToString()
        }

        internal constructor(title: CharSequence, iconResourceId: Int, value: String) {
            mTitle = title
            mAppIconResourceId = iconResourceId
            mComponentName = null
            mValue = value
        }

        override fun compareTo(other: PackageItem): Int {
            return mTitle.toString().toUpperCase(Locale.ROOT).compareTo(other.mTitle.toString().toUpperCase(
                Locale.ROOT
            )
            )
        }

        override fun hashCode(): Int {
            return mValue.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            return if (other == null || other !is PackageItem) {
                false
            } else mValue == other.mValue
        }
    }

    inner class AppSelectListAdapter(context: Context?) : BaseAdapter() {
        private val mInflater: LayoutInflater = LayoutInflater.from(context)

        override fun getCount(): Int {
            return mInstalledPackages.size
        }

        override fun getItem(position: Int): PackageItem {
            return mInstalledPackages[position]
        }

        override fun getItemId(position: Int): Long {
            return mInstalledPackages[position].hashCode().toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
            var convertView: View? = convertView
            convertView = mInflater.inflate(R.layout.applist_preference_icon, null, false)
            val holder: ViewHolder = ViewHolder()
                convertView.setTag(holder)
                holder.title = convertView.findViewById(R.id.title) as TextView
                holder.icon = convertView.findViewById(R.id.icon) as ImageView
            val applicationInfo = getItem(position)
            holder.title!!.text = applicationInfo.mTitle
            if (applicationInfo.mAppIconResourceId != 0) {
                holder.icon!!.setImageResource(applicationInfo.mAppIconResourceId)
            } else {
                val d: Drawable? = resolveAppIcon(applicationInfo)
                holder.icon!!.setImageDrawable(d)
            }
            return convertView
        }

        fun resolveApplication(componentName: ComponentName): PackageItem? {
            for (item in mInstalledPackages) {
                if (item.mComponentName != null && item.mComponentName == componentName) {
                    return item
                }
            }
            return null
        }

        private inner class ViewHolder {
            var title: TextView? = null
            var summary: TextView? = null
            var icon: ImageView? = null
        }

    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?) : super(context, null) {
        init()
    }

    fun setPackageList(installedPackages: List<PackageItem>?) {
        mInstalledPackages.clear()
        mInstalledPackages.addAll(installedPackages!!)
        addSpecialApps()
        mAdapter!!.notifyDataSetChanged()
        updatePreferenceViews()
    }

    private fun init() {
        mPm = context.packageManager
        dialogLayoutResource = R.layout.preference_dialog_applist
        layoutResource = R.layout.preference_app_select
        setNegativeButtonText(android.R.string.cancel)
        positiveButtonText = null
        setDialogTitle(R.string.choose_app)
        dialogIcon = null
        mAdapter = AppSelectListAdapter(context)
    }

    private fun addSpecialApps() {
        val cameraItem = PackageItem(
            context.resources.getString(R.string.camera_entry),
                R.drawable.ic_camera, CAMERA_ENTRY)
        mInstalledPackages.add(0, cameraItem)
        val torchItem = PackageItem(
            context.resources.getString(R.string.torch_entry),
                R.drawable.ic_flashlight, TORCH_ENTRY)
        mInstalledPackages.add(0, torchItem)
        val ambientDisplay = PackageItem(
            context.resources.getString(R.string.ambient_display_entry),
                R.drawable.ic_ambient_display, AMBIENT_DISPLAY_ENTRY)
        mInstalledPackages.add(0, ambientDisplay)
        val disabledItem = PackageItem(
            context.resources.getString(R.string.disabled_entry),
                R.drawable.ic_disabled, DISABLED_ENTRY)
        mInstalledPackages.add(0, disabledItem)
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        super.onSetInitialValue(defaultValue)
        summary = if (mTitle != null) {
            mTitle
        } else {
            context.resources.getString(R.string.not_ready_summary)
        }
        mAppIconResourceId = R.drawable.ic_disabled
        setIcon(mAppIconResourceId)
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        val list: ListView = view.findViewById(R.id.applist) as ListView
        list.adapter = mAdapter
        list.onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val info = parent.getItemAtPosition(position) as PackageItem
                value = info.mValue
                if (shouldPersist()) {
                    persistString(value)
                }
                mTitle = info.mTitle
                mAppIconDrawable = null
                mAppIconResourceId = 0
                if (info.mComponentName != null) {
                    mAppIconDrawable = resolveAppIcon(info)
                } else {
                    mAppIconResourceId = info.mAppIconResourceId
                }
                updatePreferenceViews()
                callChangeListener(value)
                dialog.dismiss()
            }
        }
    }

    private fun updatePreferenceViews() {
        var name: String?
        name = if (shouldPersist()) {
            getPersistedString(null)
        } else {
            value
        }
        mAppIconResourceId = R.drawable.ic_disabled
        if (name != null) {
            mAppIconDrawable = null
            when (name) {
                DISABLED_ENTRY -> {
                    mTitle = context.resources.getString(R.string.disabled_entry)
                    mAppIconResourceId = R.drawable.ic_disabled
                }
                TORCH_ENTRY -> {
                    mTitle = context.resources.getString(R.string.torch_entry)
                    mAppIconResourceId = R.drawable.ic_flashlight
                }
                CAMERA_ENTRY -> {
                    mTitle = context.resources.getString(R.string.camera_entry)
                    mAppIconResourceId = R.drawable.ic_camera
                }
                AMBIENT_DISPLAY_ENTRY -> {
                    mTitle = context.resources.getString(R.string.ambient_display_entry)
                    mAppIconResourceId = R.drawable.ic_ambient_display
                }
                else -> {
                    val componentName: ComponentName = ComponentName.unflattenFromString(name)!!
                    val item = mAdapter!!.resolveApplication(componentName)
                    if (item != null) {
                        mTitle = item.mTitle
                        mAppIconDrawable = resolveAppIcon(item)
                    } else {
                        mTitle = context.resources.getString(R.string.resolve_failed_summary)
                    }
                }
            }
        } else {
            mTitle = context.resources.getString(R.string.disabled_entry)
            mAppIconResourceId = R.drawable.ic_disabled
        }
        summary = mTitle
        if (mAppIconDrawable != null) {
            icon = mAppIconDrawable
        } else {
            this.setIcon(mAppIconResourceId)
        }
    }

    private val defaultActivityIcon: Drawable
        get() = ResourcesCompat.getDrawable(context.resources, android.R.drawable.sym_def_app_icon, null)!!

    private fun resolveAppIcon(item: PackageItem): Drawable? {
        var appIcon: Drawable? = null
        try {
            appIcon = mPm!!.getActivityIcon(item.mComponentName!!)
        } catch (e: NameNotFoundException) {
        }
        if (appIcon == null) {
            appIcon = defaultActivityIcon
        }
        return appIcon
    }

    companion object {
        private const val TAG = "AppSelectListPreference"
        const val TORCH_ENTRY = "torch"
        const val DISABLED_ENTRY = "disabled"
        const val CAMERA_ENTRY = "camera"
        const val MUSIC_PLAY_ENTRY = "music_play"
        const val MUSIC_PREV_ENTRY = "music_prev"
        const val MUSIC_NEXT_ENTRY = "music_next"
        const val WAKE_ENTRY = "wake"
        const val AMBIENT_DISPLAY_ENTRY = "ambient_display"
    }
}
