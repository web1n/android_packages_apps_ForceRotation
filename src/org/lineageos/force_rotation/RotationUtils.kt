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

import android.content.Context
import android.content.Intent
import android.util.Log
import org.lineageos.force_rotation.R
import org.lineageos.force_rotation.SettingsUtils.isSettingEnabled

object RotationUtils {

    private val TAG = RotationUtils::class.java.simpleName
    private const val DEBUG = true

    private fun startService(context: Context) {
        if (DEBUG) Log.d(TAG, "Starting service")
        context.startService(Intent(context, RotationService::class.java))
    }

    private fun stopService(context: Context) {
        if (DEBUG) Log.d(TAG, "Stopping service")
        context.stopService(Intent(context, RotationService::class.java))
    }

    fun checkRotateService(context: Context) {
        if (isSettingEnabled(context)) {
            startService(context)
        } else {
            stopService(context)
        }
    }

    fun getDefaultForceRotationPackages(context: Context): Set<String> {
        return context.resources.getStringArray(R.array.config_defaultForceRotationPackaegs).toSet()
    }

    fun getExtraForceRotationPackages(context: Context): Set<String> {
        return context.resources.getStringArray(R.array.config_extraForceRotationPackaegs).toSet()
    }
}
