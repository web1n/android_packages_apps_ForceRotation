/*
 * Copyright (C) 2025 The Android Open Source Project
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

import android.content.ContentProvider
import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.android.settingslib.drawer.EntriesProvider.EXTRA_SWITCH_CHECKED_STATE
import com.android.settingslib.drawer.EntriesProvider.METHOD_IS_CHECKED
import com.android.settingslib.drawer.EntriesProvider.METHOD_ON_CHECKED_CHANGED
import org.lineageos.force_rotation.SettingsUtils.getForceRotationPackages
import org.lineageos.force_rotation.SettingsUtils.isSettingEnabled
import org.lineageos.force_rotation.SettingsUtils.setSettingEnabled

class SettingsProvider : ContentProvider() {

    override fun onCreate() = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (DEBUG) Log.d(TAG, "method: $method arg: $arg extras: $extras")

        return Bundle().apply {
            when (method) {
                METHOD_IS_CHECKED -> putBoolean(
                    EXTRA_SWITCH_CHECKED_STATE,
                    isSettingEnabled(requireContext())
                )

                METHOD_ON_CHECKED_CHANGED -> extras?.getBoolean(EXTRA_SWITCH_CHECKED_STATE)?.let {
                    setSettingEnabled(requireContext(), it)
                }

                METHOD_GET_FORCE_ROTATION_PACKAGES -> putStringArray(
                    EXTRA_FORCE_ROTATION_PACKAGES,
                    getForceRotationPackages(requireContext()).toTypedArray()
                )

                else -> Log.w(TAG, "Unsupported method: $method")
            }
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ) = null

    override fun getType(uri: Uri) = null
    override fun insert(uri: Uri, values: ContentValues?) = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?) = 0
    override fun update(
        uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?
    ) = 0

    companion object {
        private const val DEBUG = true
        private val TAG = SettingsProvider::class.java.simpleName

        const val METHOD_GET_FORCE_ROTATION_PACKAGES = "getForceRotationPackages"
        const val EXTRA_FORCE_ROTATION_PACKAGES = "force_rotation_packages"
    }
}
