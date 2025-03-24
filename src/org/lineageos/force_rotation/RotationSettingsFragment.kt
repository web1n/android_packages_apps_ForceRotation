/*
 * Copyright (C) 2023-2025 The LineageOS Project
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

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CompoundButton
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.android.settingslib.widget.MainSwitchPreference
import org.lineageos.force_rotation.R
import org.lineageos.force_rotation.RotationUtils.checkRotateService
import org.lineageos.force_rotation.SettingsUtils.KEY_FORCE_ROTATION_ENABLED
import org.lineageos.force_rotation.SettingsUtils.getForceRotationPackages
import org.lineageos.force_rotation.SettingsUtils.setForceRotationPackages

class RotationSettingsFragment : PreferenceFragmentCompat(),
    Preference.OnPreferenceChangeListener, CompoundButton.OnCheckedChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.rotation_settings)

        findPreference<MainSwitchPreference?>(KEY_FORCE_ROTATION_ENABLED)?.apply {
            addOnSwitchChangeListener(this)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadApplicationList()
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        context?.let { checkRotateService(it) }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        if (preference !is SwitchPreferenceCompat || newValue !is Boolean) {
            return false
        }
        if (DEBUG) Log.d(TAG, "Set package ${preference.key} force rotation: $newValue")

        preference.isChecked = newValue
        updateSelectedApps()

        return false
    }

    private fun updateSelectedApps() {
        val appListCategory = findPreference<PreferenceCategory>(CATEGORY_APP_LIST) ?: return
        val selectedPackages = (0 until appListCategory.preferenceCount)
            .map { appListCategory.getPreference(it) }
            .filterIsInstance<SwitchPreferenceCompat>()
            .filter { it.isChecked }
            .map { it.key }
            .toSet()

        setForceRotationPackages(requireContext(), selectedPackages)
    }

    private fun loadApplicationList() {
        val category = findPreference<PreferenceCategory>(CATEGORY_APP_LIST)!!
        val storedPackages = getForceRotationPackages(requireContext())

        category.removeAll()

        getLaunchableUserApps().map { appInfo ->
            SwitchPreferenceCompat(requireContext()).apply {
                key = appInfo.packageName
                title = appInfo.loadLabel(requireContext().packageManager).toString()
                summary = appInfo.packageName
                isChecked = storedPackages.contains(appInfo.packageName)
                onPreferenceChangeListener = this@RotationSettingsFragment
            }
        }.forEach {
            category.addPreference(it)
        }
    }

    private fun getLaunchableUserApps(): List<ApplicationInfo> {
        return requireContext().packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }, 0
        ).map {
            it.activityInfo.applicationInfo
        }.filter { appInfo ->
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
        }
    }

    companion object {
        private val TAG = RotationSettingsFragment::class.java.simpleName
        private const val DEBUG = true

        private const val CATEGORY_APP_LIST = "app_list"
    }
}
