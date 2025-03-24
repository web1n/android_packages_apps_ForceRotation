/*
 * Copyright (C) 2025 The LineageOS Project
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

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import org.lineageos.force_rotation.RotationUtils.getDefaultForceRotationPackages
import org.lineageos.force_rotation.RotationUtils.getExtraForceRotationPackages
import org.lineageos.force_rotation.SettingsProvider.Companion.EXTRA_FORCE_ROTATION_PACKAGES
import org.lineageos.force_rotation.SettingsProvider.Companion.METHOD_GET_FORCE_ROTATION_PACKAGES

object SettingsUtils {

    private const val DEBUG = true
    private val TAG = SettingsUtils::class.java.simpleName

    const val AUTHORITY_SETTINGS = "org.lineageos.force_rotation.settings"

    const val KEY_FORCE_ROTATION_ENABLED = "force_rotation_enabled"
    const val KEY_FORCE_ROTATION_PACKAGES = "force_rotation_packages"

    val FORCE_ROTATION_PACKAGES_CONFIG_URI: Uri = Uri.Builder()
        .scheme(ContentResolver.SCHEME_CONTENT)
        .authority(AUTHORITY_SETTINGS)
        .appendPath(METHOD_GET_FORCE_ROTATION_PACKAGES)
        .build()

    fun isSettingEnabled(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_FORCE_ROTATION_ENABLED, false)
    }

    fun setSettingEnabled(context: Context, enabled: Boolean) {
        if (DEBUG) Log.d(TAG, "setSettingEnabled: $enabled")

        // put to shared preference
        PreferenceManager.getDefaultSharedPreferences(context).edit(true) {
            putBoolean(KEY_FORCE_ROTATION_ENABLED, enabled)
        }

        RotationUtils.checkRotateService(context)
    }

    fun setForceRotationPackages(context: Context, packages: Set<String>) {
        if (DEBUG) Log.d(TAG, "setForceRotationPackages: ${packages.joinToString()}")

        PreferenceManager.getDefaultSharedPreferences(context).edit(true) {
            putStringSet(KEY_FORCE_ROTATION_PACKAGES, packages)
        }

        context.contentResolver.notifyChange(FORCE_ROTATION_PACKAGES_CONFIG_URI, null)
    }

    fun getForceRotationPackages(context: Context): Set<String> {
        val packages = PreferenceManager.getDefaultSharedPreferences(context).getStringSet(
            KEY_FORCE_ROTATION_PACKAGES,
            getDefaultForceRotationPackages(context)
        )?.toMutableList() ?: mutableListOf()

        // add extra packages
        packages.addAll(getExtraForceRotationPackages(context))

        return packages.toSet()
    }

    fun getForceRotationPackages(contentResolver: ContentResolver): Set<String> {
        return contentResolver.call(
            AUTHORITY_SETTINGS, METHOD_GET_FORCE_ROTATION_PACKAGES, null, null
        )?.getStringArray(EXTRA_FORCE_ROTATION_PACKAGES)?.toSet() ?: emptySet()
    }

}
