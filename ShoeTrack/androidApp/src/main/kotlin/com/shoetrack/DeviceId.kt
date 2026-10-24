package com.shoetrack

import android.content.Context
import java.util.UUID

/**
 * Identifies this installation in sync metadata. Generated once and kept in
 * SharedPreferences, so every record written here carries the same id.
 */
internal object DeviceId {

    private const val PREFERENCES_NAME = "shoetrack_device"
    private const val KEY_DEVICE_ID = "device_id"

    fun getOrCreate(context: Context): String {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        return preferences.getString(KEY_DEVICE_ID, null) ?: UUID.randomUUID().toString().also {
            preferences.edit().putString(KEY_DEVICE_ID, it).apply()
        }
    }
}
