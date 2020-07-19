/*
* Copyright (C) 2016 The OmniROM Project
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

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.*
import android.database.ContentObserver
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.IAudioService
import android.media.session.MediaSessionLegacyHelper
import android.net.Uri
import android.os.*
import android.os.PowerManager.WakeLock
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.WindowManagerGlobal
import com.aicp.device.GestureSettings
import com.aicp.device.KeyHandler.ClientPackageNameObserver
import com.aicp.device.KeyHandler.SettingsObserver
import com.aicp.device.Utils.fileWritable
import com.aicp.device.Utils.getFileValue
import com.aicp.device.Utils.writeValue
import com.android.internal.statusbar.IStatusBarService
import com.android.internal.util.ArrayUtils
import com.android.internal.util.aicp.AicpUtils
import com.android.internal.util.aicp.AicpVibe
import com.android.internal.util.aicp.DeviceKeyHandler
import com.android.internal.util.aicp.PackageUtils

class KeyHandler(val mContext: Context) : DeviceKeyHandler {
    private val mPowerManager: PowerManager
    private val mEventHandler: EventHandler
    private val mGestureWakeLock: WakeLock
    private val mHandler = Handler()
    private val mSettingsObserver: SettingsObserver
    private val mNoMan: NotificationManager
    private val mAudioManager: AudioManager
    private val mSensorManager: SensorManager
    private var mProxyIsNear = false
    private var mUseProxiCheck = false
    private val mTiltSensor: Sensor?
    private var mUseTiltCheck = false
    private var mProxyWasNear = false
    private var mProxySensorTimestamp: Long = 0
    private var mUseWaveCheck = false
    private val mPocketSensor: Sensor?
    private var mUsePocketCheck = false
    private val mFPcheck = false
    private var mDispOn = true
    private val isFpgesture = false
    private var mClientObserver: ClientPackageNameObserver? = null
    private var mRestoreUser = false
    private var mUseSliderTorch = false
    private var mTorchState = false
    private var mDoubleTapToWake = false
    private val mProximitySensor: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            mProxyIsNear = event.values[0] == 1F
            Log.i(
                TAG,
                "mProxyIsNear = $mProxyIsNear mProxyWasNear = $mProxyWasNear"
            )
            if (mUseProxiCheck) {
                if (fileWritable(GOODIX_CONTROL_PATH)) {
                    writeValue(
                        GOODIX_CONTROL_PATH,
                        if (mProxyIsNear) "1" else "0"
                    )
                }
            }
            if (mUseWaveCheck || mUsePocketCheck) {
                if (mProxyWasNear && !mProxyIsNear) {
                    val delta =
                        SystemClock.elapsedRealtime() - mProxySensorTimestamp
                    Log.i(
                        TAG,
                        "delta = $delta"
                    )
                    if (mUseWaveCheck && delta < HANDWAVE_MAX_DELTA_MS) {
                        launchDozePulse()
                    }
                    if (mUsePocketCheck && delta > POCKET_MIN_DELTA_MS) {
                        launchDozePulse()
                    }
                }
                mProxySensorTimestamp = SystemClock.elapsedRealtime()
                mProxyWasNear = mProxyIsNear
            }
        }

        override fun onAccuracyChanged(
            sensor: Sensor,
            accuracy: Int
        ) {
        }
    }
    private val mTiltSensorListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.values[0] == 1F) {
                launchDozePulse()
            }
        }

        override fun onAccuracyChanged(
            sensor: Sensor,
            accuracy: Int
        ) {
        }
    }

    private inner class SettingsObserver internal constructor(handler: Handler?) :
        ContentObserver(handler) {
        fun observe() {
            mContext.contentResolver.registerContentObserver(
                Settings.System.getUriFor(
                    Settings.System.OMNI_DEVICE_PROXI_CHECK_ENABLED
                ),
                false, this
            )
            mContext.contentResolver.registerContentObserver(
                Settings.System.getUriFor(
                    Settings.System.OMNI_DEVICE_FEATURE_SETTINGS
                ),
                false, this
            )
            mContext.contentResolver.registerContentObserver(
                Settings.Secure.getUriFor(
                    Settings.Secure.DOUBLE_TAP_TO_WAKE
                ),
                false, this
            )
            update()
            updateDozeSettings()
        }

        override fun onChange(selfChange: Boolean) {
            update()
        }

        override fun onChange(
            selfChange: Boolean,
            uri: Uri
        ) {
            if (uri == Settings.System.getUriFor(
                    Settings.System.OMNI_DEVICE_FEATURE_SETTINGS
                )
            ) {
                updateDozeSettings()
                return
            }
            update()
        }

        fun update() {
            mUseProxiCheck = Settings.System.getIntForUser(
                mContext.contentResolver,
                Settings.System.OMNI_DEVICE_PROXI_CHECK_ENABLED,
                1,
                UserHandle.USER_CURRENT
            ) == 1
            mDoubleTapToWake = Settings.Secure.getIntForUser(
                mContext.contentResolver,
                Settings.Secure.DOUBLE_TAP_TO_WAKE,
                0,
                UserHandle.USER_CURRENT
            ) == 1
            updateDoubleTapToWake()
        }
    }

    private val mSystemStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            if (intent.action == Intent.ACTION_SCREEN_ON) {
                mDispOn = true
                onDisplayOn()
            } else if (intent.action == Intent.ACTION_SCREEN_OFF) {
                mDispOn = false
                onDisplayOff()
            } else if (intent.action == Intent.ACTION_USER_SWITCHED) {
                val userId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, UserHandle.USER_NULL)
                if (userId == UserHandle.USER_SYSTEM && mRestoreUser) {
                    Log.i(
                        TAG,
                        "ACTION_USER_SWITCHED to system"
                    )
                    Startup.restoreAfterUserSwitch(context)
                } else {
                    mRestoreUser = true
                }
            }
        }
    }

    private inner class EventHandler : Handler() {
        override fun handleMessage(msg: Message) {}
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        return if (event.action != KeyEvent.ACTION_UP) {
            false
        } else false
    }

    fun canHandleKeyEvent(event: KeyEvent): Boolean {
        return ArrayUtils.contains(
            sSupportedGestures,
            event.scanCode
        )
    }

    fun isDisabledKeyEvent(event: KeyEvent): Boolean {
        val isProxyCheckRequired = mUseProxiCheck &&
                ArrayUtils.contains(
                    sProxiCheckedGestures,
                    event.scanCode
                )
        if (mProxyIsNear && isProxyCheckRequired) {
            Log.i(
                TAG,
                "isDisabledKeyEvent: blocked by proxi sensor - scanCode=" + event.scanCode
            )
            return true
        }
        return false
    }

    fun isCameraLaunchEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_UP) {
            return false
        }
        val value = getGestureValueForScanCode(event.scanCode)
        return !TextUtils.isEmpty(value) && value == AppSelectListPreference.CAMERA_ENTRY
    }

    fun isWakeEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_UP) {
            return false
        }
        val value = getGestureValueForScanCode(event.scanCode)
        if (!TextUtils.isEmpty(value) && value == AppSelectListPreference.WAKE_ENTRY) {
            Log.i(
                TAG,
                "isWakeEvent " + event.scanCode + value
            )
            return true
        }
        return event.scanCode == KEY_DOUBLE_TAP
    }

    fun isActivityLaunchEvent(event: KeyEvent): Intent? {
        if (event.action != KeyEvent.ACTION_UP) {
            return null
        }
        val value = getGestureValueForScanCode(event.scanCode)
        if (!TextUtils.isEmpty(value) && value != AppSelectListPreference.DISABLED_ENTRY) {
            Log.i(
                TAG,
                "isActivityLaunchEvent " + event.scanCode + value
            )
            if (!launchSpecialActions(value)) {
                AicpVibe.performHapticFeedbackLw(
                    HapticFeedbackConstants.LONG_PRESS,
                    false,
                    mContext,
                    GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                    GESTURE_HAPTIC_DURATION
                )
                return createIntent(value)
            }
        }
        return null
    }

    private val audioService: IAudioService?
        get() {
            val audioService: IAudioService = IAudioService.Stub
                .asInterface(ServiceManager.checkService(Context.AUDIO_SERVICE))
            if (audioService == null) {
                Log.w(
                    TAG,
                    "Unable to find IAudioService interface."
                )
            }
            return audioService
        }

    val isMusicActive: Boolean
        get() = mAudioManager.isMusicActive

    private fun dispatchMediaKeyWithWakeLockToAudioService(keycode: Int) {
        if (ActivityManager.isSystemReady()) {
            val audioService: IAudioService? = audioService
            if (audioService != null) {
                var event = KeyEvent(
                    SystemClock.uptimeMillis(),
                    SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN,
                    keycode, 0
                )
                dispatchMediaKeyEventUnderWakelock(event)
                event = KeyEvent.changeAction(event, KeyEvent.ACTION_UP)
                dispatchMediaKeyEventUnderWakelock(event)
            }
        }
    }

    private fun dispatchMediaKeyEventUnderWakelock(event: KeyEvent) {
        if (ActivityManager.isSystemReady()) {
            MediaSessionLegacyHelper.getHelper(mContext).sendMediaButtonEvent(event, true)
        }
    }

    private fun onDisplayOn() {
        Log.i(
            TAG,
            "Display on"
        )
        if (enableProxiSensor()) {
            mSensorManager.unregisterListener(mProximitySensor, mPocketSensor)
            enableGoodix()
        }
        if (mUseTiltCheck) {
            mSensorManager.unregisterListener(mTiltSensorListener, mTiltSensor)
        }
    }

    private fun enableGoodix() {
        if (fileWritable(GOODIX_CONTROL_PATH)) {
            writeValue(
                GOODIX_CONTROL_PATH,
                "0"
            )
        }
    }

    private fun onDisplayOff() {
        Log.i(
            TAG,
            "Display off"
        )
        if (enableProxiSensor()) {
            mProxyWasNear = false
            mSensorManager.registerListener(
                mProximitySensor, mPocketSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            mProxySensorTimestamp = SystemClock.elapsedRealtime()
        }
        if (mUseTiltCheck) {
            mSensorManager.registerListener(
                mTiltSensorListener, mTiltSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        if (mClientObserver != null) {
            mClientObserver!!.stopWatching()
            mClientObserver = null
        }
    }

    private fun getSliderAction(position: Int): Int {
        var value: String? = Settings.System.getStringForUser(
            mContext.contentResolver,
            Settings.System.OMNI_BUTTON_EXTRA_KEY_MAPPING,
            UserHandle.USER_CURRENT
        )
        val defaultValue = DeviceSettings.SLIDER_DEFAULT_VALUE
        if (value == null) {
            value = defaultValue
        } else if (value.indexOf(",") == -1) {
            value = defaultValue
        }
        try {
            val parts = value.split(",".toRegex()).toTypedArray()
            return Integer.valueOf(parts[position])
        } catch (e: Exception) {
        }
        return 0
    }

    private fun doHandleSliderAction(position: Int) {
        val action = getSliderAction(position)
        if (action == 0) {
            mNoMan.setZenMode(ZEN_MODE_OFF, null, TAG)
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL)
            mTorchState = false
        } else if (action == 1) {
            mNoMan.setZenMode(ZEN_MODE_OFF, null, TAG)
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_VIBRATE)
            mTorchState = false
        } else if (action == 2) {
            mNoMan.setZenMode(ZEN_MODE_OFF, null, TAG)
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_SILENT)
            mTorchState = false
        } else if (action == 3) {
            mNoMan.setZenMode(
                ZEN_MODE_IMPORTANT_INTERRUPTIONS,
                null,
                TAG
            )
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL)
            mTorchState = false
        } else if (action == 4) {
            mNoMan.setZenMode(ZEN_MODE_OFF, null, TAG)
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL)
            mUseSliderTorch = true
            mTorchState = true
        }
        if ((!mProxyIsNear && mUseProxiCheck || !mUseProxiCheck) && mUseSliderTorch && action < 4) {
            launchSpecialActions(AppSelectListPreference.TORCH_ENTRY)
            mUseSliderTorch = false
        } else if ((!mProxyIsNear && mUseProxiCheck || !mUseProxiCheck) && mUseSliderTorch) {
            launchSpecialActions(AppSelectListPreference.TORCH_ENTRY)
        }
    }

    private fun createIntent(value: String?): Intent {
        val componentName = ComponentName.unflattenFromString(value!!)
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.flags = (Intent.FLAG_ACTIVITY_NEW_TASK
                or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        intent.component = componentName
        return intent
    }

    private fun launchSpecialActions(value: String?): Boolean {
        if (value == AppSelectListPreference.TORCH_ENTRY) {
            mGestureWakeLock.acquire(GESTURE_WAKELOCK_DURATION.toLong())
            val service: IStatusBarService = statusBarService
            if (service != null) {
                try {
                    service.toggleCameraFlash()
                    AicpVibe.performHapticFeedbackLw(
                        HapticFeedbackConstants.LONG_PRESS,
                        false,
                        mContext,
                        GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                        GESTURE_HAPTIC_DURATION
                    )
                    return true
                } catch (e: RemoteException) {
                    // do nothing.
                }
            }
        } else if (value == AppSelectListPreference.MUSIC_PLAY_ENTRY) {
            mGestureWakeLock.acquire(GESTURE_WAKELOCK_DURATION.toLong())
            AicpVibe.performHapticFeedbackLw(
                HapticFeedbackConstants.LONG_PRESS,
                false,
                mContext,
                GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                GESTURE_HAPTIC_DURATION
            )
            dispatchMediaKeyWithWakeLockToAudioService(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            return true
        } else if (value == AppSelectListPreference.MUSIC_NEXT_ENTRY) {
            if (isMusicActive) {
                mGestureWakeLock.acquire(GESTURE_WAKELOCK_DURATION.toLong())
                AicpVibe.performHapticFeedbackLw(
                    HapticFeedbackConstants.LONG_PRESS,
                    false,
                    mContext,
                    GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                    GESTURE_HAPTIC_DURATION
                )
                dispatchMediaKeyWithWakeLockToAudioService(KeyEvent.KEYCODE_MEDIA_NEXT)
            }
            return true
        } else if (value == AppSelectListPreference.MUSIC_PREV_ENTRY) {
            if (isMusicActive) {
                mGestureWakeLock.acquire(GESTURE_WAKELOCK_DURATION.toLong())
                AicpVibe.performHapticFeedbackLw(
                    HapticFeedbackConstants.LONG_PRESS,
                    false,
                    mContext,
                    GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                    GESTURE_HAPTIC_DURATION
                )
                dispatchMediaKeyWithWakeLockToAudioService(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            }
            return true
        } else if (value == AppSelectListPreference.AMBIENT_DISPLAY_ENTRY) {
            AicpVibe.performHapticFeedbackLw(
                HapticFeedbackConstants.LONG_PRESS,
                false,
                mContext,
                GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME,
                GESTURE_HAPTIC_DURATION
            )
            launchDozePulse()
            return true
        }
        return false
    }

    private fun getGestureValueForScanCode(scanCode: Int): String? {
        when (scanCode) {
            GESTURE_II_SCANCODE -> return Settings.System.getStringForUser(
                mContext.contentResolver,
                GestureSettings.DEVICE_GESTURE_MAPPING_0, UserHandle.USER_CURRENT
            )
            GESTURE_CIRCLE_SCANCODE -> return Settings.System.getStringForUser(
                mContext.contentResolver,
                GestureSettings.DEVICE_GESTURE_MAPPING_1, UserHandle.USER_CURRENT
            )
            GESTURE_V_SCANCODE -> return Settings.System.getStringForUser(
                mContext.contentResolver,
                GestureSettings.DEVICE_GESTURE_MAPPING_2, UserHandle.USER_CURRENT
            )
            GESTURE_A_SCANCODE -> return Settings.System.getStringForUser(
                mContext.contentResolver,
                GestureSettings.DEVICE_GESTURE_MAPPING_3, UserHandle.USER_CURRENT
            )
            GESTURE_LEFT_V_SCANCODE -> return Settings.System.getStringForUser(
                mContext.contentResolver,
                GestureSettings.DEVICE_GESTURE_MAPPING_4, UserHandle.USER_CURRENT
            )
            GESTURE_RIGHT_V_SCANCODE -> return Settings.System.getStringForUser(
                mContext.contentResolver,
                GestureSettings.DEVICE_GESTURE_MAPPING_5, UserHandle.USER_CURRENT
            )
            GESTURE_DOWN_SWIPE_SCANCODE -> return Settings.System.getStringForUser(
                mContext.contentResolver,
                GestureSettings.DEVICE_GESTURE_MAPPING_6, UserHandle.USER_CURRENT
            )
            GESTURE_UP_SWIPE_SCANCODE -> return Settings.System.getStringForUser(
                mContext.contentResolver,
                GestureSettings.DEVICE_GESTURE_MAPPING_7, UserHandle.USER_CURRENT
            )
            GESTURE_LEFT_SWIPE_SCANCODE -> return Settings.System.getStringForUser(
                mContext.contentResolver,
                GestureSettings.DEVICE_GESTURE_MAPPING_8, UserHandle.USER_CURRENT
            )
            GESTURE_RIGHT_SWIPE_SCANCODE -> return Settings.System.getStringForUser(
                mContext.contentResolver,
                GestureSettings.DEVICE_GESTURE_MAPPING_9, UserHandle.USER_CURRENT
            )
        }
        return null
    }

    private fun launchDozePulse() {
        Log.i(
            TAG,
            "Doze pulse"
        )
        mContext.sendBroadcastAsUser(
            Intent(DOZE_INTENT),
            UserHandle(UserHandle.USER_CURRENT)
        )
    }

    private fun enableProxiSensor(): Boolean {
        return mUsePocketCheck || mUseWaveCheck || mUseProxiCheck
    }

    private fun updateDozeSettings() {
        val value: String = Settings.System.getStringForUser(
            mContext.contentResolver,
            Settings.System.OMNI_DEVICE_FEATURE_SETTINGS,
            UserHandle.USER_CURRENT
        )
        Log.i(
            TAG,
            "Doze settings = $value"
        )
        if (!TextUtils.isEmpty(value)) {
            val parts = value.split(":".toRegex()).toTypedArray()
            mUseWaveCheck = java.lang.Boolean.valueOf(parts[0])
            mUsePocketCheck = java.lang.Boolean.valueOf(parts[1])
            mUseTiltCheck = java.lang.Boolean.valueOf(parts[2])
        }
    }

    val statusBarService: IStatusBarService
        get() = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"))

    fun getCustomProxiIsNear(event: SensorEvent): Boolean {
        return event.values[0] == 1F
    }

    val customProxiSensor: String
        get() = "oneplus.sensor.pocket"

    private inner class ClientPackageNameObserver(file: String?) : FileObserver(
        CLIENT_PACKAGE_PATH,
        MODIFY
    ) {
        override fun onEvent(event: Int, file: String?) {
            val pkgName = getFileValue(
                CLIENT_PACKAGE_PATH,
                "0"
            )
        }
    }

    private fun updateDoubleTapToWake() {
        Log.i(
            TAG,
            "udateDoubleTapToWake $mDoubleTapToWake"
        )
        if (fileWritable(DT2W_CONTROL_PATH)) {
            writeValue(
                DT2W_CONTROL_PATH,
                if (mDoubleTapToWake) "1" else "0"
            )
        }
    }

    companion object {
        private const val TAG = "KeyHandler"
        private const val DEBUG = true
        private const val DEBUG_SENSOR = true
        const val GESTURE_REQUEST = 1
        private const val GESTURE_WAKELOCK_DURATION = 2000
        const val GESTURE_HAPTIC_SETTINGS_VARIABLE_NAME = "OFF_GESTURE_HAPTIC_ENABLE"
        private const val GESTURE_HAPTIC_DURATION = 50
        private const val GOODIX_CONTROL_PATH =
            "/sys/devices/platform/soc/soc:goodix_fp/proximity_state"
        private const val DT2W_CONTROL_PATH = "/proc/touchpanel/double_tap_enable"
        private const val GESTURE_CIRCLE_SCANCODE = 250
        private const val GESTURE_V_SCANCODE = 252
        private const val GESTURE_II_SCANCODE = 251
        private const val GESTURE_LEFT_V_SCANCODE = 253
        private const val GESTURE_RIGHT_V_SCANCODE = 254
        private const val GESTURE_A_SCANCODE = 255
        private const val GESTURE_RIGHT_SWIPE_SCANCODE = 63
        private const val GESTURE_LEFT_SWIPE_SCANCODE = 64
        private const val GESTURE_DOWN_SWIPE_SCANCODE = 65
        private const val GESTURE_UP_SWIPE_SCANCODE = 66
        private const val KEY_DOUBLE_TAP = 143
        private const val KEY_HOME = 102
        private const val KEY_BACK = 158
        private const val KEY_RECENTS = 580
        private const val KEY_SLIDER_TOP = 601
        private const val KEY_SLIDER_CENTER = 602
        private const val KEY_SLIDER_BOTTOM = 603
        private const val MIN_PULSE_INTERVAL_MS = 2500
        private const val DOZE_INTENT = "com.android.systemui.doze.pulse"
        private const val HANDWAVE_MAX_DELTA_MS = 1000
        private const val POCKET_MIN_DELTA_MS = 5000
        private const val FP_GESTURE_LONG_PRESS = 305
        const val CLIENT_PACKAGE_NAME = "com.oneplus.camera"
        const val CLIENT_PACKAGE_PATH = "/data/misc/omni/client_package_name"
        private val sSupportedGestures = intArrayOf(
            GESTURE_II_SCANCODE,
            GESTURE_CIRCLE_SCANCODE,
            GESTURE_V_SCANCODE,
            GESTURE_A_SCANCODE,
            GESTURE_LEFT_V_SCANCODE,
            GESTURE_RIGHT_V_SCANCODE,
            GESTURE_DOWN_SWIPE_SCANCODE,
            GESTURE_UP_SWIPE_SCANCODE,
            GESTURE_LEFT_SWIPE_SCANCODE,
            GESTURE_RIGHT_SWIPE_SCANCODE,
            KEY_DOUBLE_TAP,
            KEY_SLIDER_TOP,
            KEY_SLIDER_CENTER,
            KEY_SLIDER_BOTTOM
        )
        private val sProxiCheckedGestures = intArrayOf(
            GESTURE_II_SCANCODE,
            GESTURE_CIRCLE_SCANCODE,
            GESTURE_V_SCANCODE,
            GESTURE_A_SCANCODE,
            GESTURE_LEFT_V_SCANCODE,
            GESTURE_RIGHT_V_SCANCODE,
            GESTURE_DOWN_SWIPE_SCANCODE,
            GESTURE_UP_SWIPE_SCANCODE,
            GESTURE_LEFT_SWIPE_SCANCODE,
            GESTURE_RIGHT_SWIPE_SCANCODE,
            KEY_DOUBLE_TAP
        )
        private const val mButtonDisabled = false
        fun getSensor(sm: SensorManager, type: String): Sensor? {
            for (sensor in sm.getSensorList(Sensor.TYPE_ALL)) {
                if (type == sensor.stringType) {
                    return sensor
                }
            }
            return null
        }
    }

    init {
        mEventHandler = EventHandler()
        mPowerManager =
            mContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        mGestureWakeLock = mPowerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "GestureWakeLock"
        )
        mSettingsObserver = SettingsObserver(mHandler)
        mSettingsObserver.observe()
        mNoMan =
            mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mAudioManager =
            mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mSensorManager =
            mContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mTiltSensor = getSensor(
            mSensorManager,
            "oneplus.sensor.op_motion_detect"
        )
        mPocketSensor =
            getSensor(mSensorManager, "oneplus.sensor.pocket")
        val systemStateFilter = IntentFilter(Intent.ACTION_SCREEN_ON)
        systemStateFilter.addAction(Intent.ACTION_SCREEN_OFF)
        systemStateFilter.addAction(Intent.ACTION_USER_SWITCHED)
        mContext.registerReceiver(mSystemStateReceiver, systemStateFilter)
        object : UEventObserver() {
            fun onUEvent(event: UEventObserver.UEvent) {
                try {
                    val state: String = event.get("STATE")
                    val ringing = state.contains("USB=0")
                    val silent = state.contains("(null)=0")
                    val vibrate = state.contains("USB_HOST=0")
                    Log.v(
                        TAG,
                        "Got ringing = $ringing, silent = $silent, vibrate = $vibrate"
                    )
                    if (ringing && !silent && !vibrate) doHandleSliderAction(2)
                    if (silent && !ringing && !vibrate) doHandleSliderAction(0)
                    if (vibrate && !silent && !ringing) doHandleSliderAction(1)
                } catch (e: Exception) {
                    Log.d(
                        TAG,
                        "Failed parsing uevent",
                        e
                    )
                }
            }
        }.startObserving("DEVPATH=/devices/platform/soc/soc:tri_state_key")
    }
}
