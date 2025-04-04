/*
 * Copyright (C) 2023,2025 The LineageOS Project
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
package org.lineageos.force_rotation

import android.app.ActivityManager.RunningTaskInfo
import android.app.ActivityTaskManager
import android.app.Service
import android.app.TaskStackListener
import android.content.Intent
import android.content.pm.ActivityInfo
import android.database.ContentObserver
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import android.view.View
import android.view.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lineageos.force_rotation.SettingsUtils.FORCE_ROTATION_PACKAGES_CONFIG_URI
import org.lineageos.force_rotation.SettingsUtils.getForceRotationPackages

class RotationService : Service() {

    private val activityTaskManager by lazy { ActivityTaskManager.getService() }
    private val windowManager by lazy { getSystemService(WindowManager::class.java) }

    private val forceRotatePackages = mutableSetOf<String>()

    private var forceRotationPackageObserver: ContentObserver? = null

    private var overlayView: View? = null
    private lateinit var layoutParams: WindowManager.LayoutParams

    private var taskStackListener: TaskStackListener? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        if (DEBUG) Log.d(TAG, "onCreate")
        super.onCreate()

        initializeOverlayView()
        initializeForceRotationPackages()
        initializeTaskStackListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "onStartCommand")
        return START_STICKY
    }

    override fun onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy")
        super.onDestroy()

        unregisterTaskStackListener()
        unregisterContentObserver()
        destroyOverlayView()

        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun initializeOverlayView() {
        overlayView = View(applicationContext)
        layoutParams = WindowManager.LayoutParams().apply {
            title = "ForceRotationOverlay"
            type = WindowManager.LayoutParams.TYPE_SECURE_SYSTEM_OVERLAY
            flags = (WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
            privateFlags = WindowManager.LayoutParams.PRIVATE_FLAG_TRUSTED_OVERLAY
            width = 0
            height = 0
            format = PixelFormat.TRANSLUCENT
            screenOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
        }

        try {
            windowManager.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay view.", e)
        }
    }

    private fun destroyOverlayView() {
        if (overlayView?.isAttachedToWindow == true) {
            try {
                windowManager.removeViewImmediate(overlayView)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove overlay view", e)
            }
        }

        overlayView = null
    }

    private fun initializeForceRotationPackages() {
        updateForceRotationPackages()

        forceRotationPackageObserver = object : ContentObserver(Handler(mainLooper)) {
            override fun onChange(selfChange: Boolean) {
                if (DEBUG) Log.d(TAG, "Force rotation package list changed")
                updateForceRotationPackages()
            }
        }

        try {
            contentResolver.registerContentObserver(
                FORCE_ROTATION_PACKAGES_CONFIG_URI, false, forceRotationPackageObserver!!
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register content observer", e)
        }
    }

    private fun unregisterContentObserver() {
        forceRotationPackageObserver?.let {
            try {
                contentResolver.unregisterContentObserver(it)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister content observer", e)
            }
        }
        forceRotationPackageObserver = null
    }

    private fun updateForceRotationPackages() {
        serviceScope.launch {
            try {
                val updatedPackages = withContext(Dispatchers.IO) {
                    getForceRotationPackages(contentResolver)
                }
                if (DEBUG) Log.d(TAG, "Force rotation packages: ${updatedPackages.joinToString()}")

                synchronized(forceRotatePackages) {
                    forceRotatePackages.clear()
                    forceRotatePackages.addAll(updatedPackages)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update force rotation packages", e)
            }
        }
    }

    private fun initializeTaskStackListener() {
        taskStackListener = object : TaskStackListener() {
            private var previousPackageName: String? = null

            override fun onTaskMovedToFront(info: RunningTaskInfo) {
                val packageName = info.topActivity?.packageName ?: return
                if (packageName == previousPackageName) return
                previousPackageName = packageName

                updateScreenOrientation(packageName)
            }
        }

        try {
            activityTaskManager.registerTaskStackListener(taskStackListener)
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to register task stack listener", e)
        }
    }

    private fun unregisterTaskStackListener() {
        taskStackListener?.let {
            try {
                activityTaskManager.unregisterTaskStackListener(it)
            } catch (e: RemoteException) {
                Log.e(TAG, "Failed to unregister task stack listener", e)
            }
        }
        taskStackListener = null
    }

    private fun updateScreenOrientation(packageName: String) {
        serviceScope.launch {
            val forceRotate = synchronized(forceRotatePackages) {
                forceRotatePackages.contains(packageName)
            }
            val newOrientation = if (forceRotate) {
                ActivityInfo.SCREEN_ORIENTATION_USER
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }

            if (newOrientation == layoutParams.screenOrientation) {
                return@launch
            }

            if (DEBUG) Log.d(TAG, "Update newOrientation: $newOrientation")
            layoutParams.screenOrientation = newOrientation

            if (overlayView?.isAttachedToWindow == true) {
                windowManager.updateViewLayout(overlayView, layoutParams)
            }
        }
    }

    companion object {
        private val TAG = RotationService::class.java.simpleName
        private const val DEBUG = true
    }
}
