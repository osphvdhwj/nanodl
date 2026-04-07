package com.yourname.nanodl.utils

import android.content.Context
import android.content.SharedPreferences

class ConfigManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("NanoConfig", Context.MODE_PRIVATE)

    var outputTemplate: String
        get() = prefs.getString("OUTPUT_TEMPLATE", "%(title)s-%(id)s.%(ext)s") ?: "%(title)s-%(id)s.%(ext)s"
        set(value) = prefs.edit().putString("OUTPUT_TEMPLATE", value).apply()

    var downloadSubtitles: Boolean
        get() = prefs.getBoolean("DOWNLOAD_SUBS", false)
        set(value) = prefs.edit().putBoolean("DOWNLOAD_SUBS", value).apply()

    // THE FIX: Added Storage URI persistence
    var customStorageUri: String?
        get() = prefs.getString("STORAGE_URI", null)
        set(value) = prefs.edit().putString("STORAGE_URI", value).apply()

    fun buildFileName(title: String, id: String, resolution: String, ext: String): String {
        val safeTitle = title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        return outputTemplate
            .replace("%(title)s", safeTitle)
            .replace("%(id)s", id)
            .replace("%(resolution)s", resolution)
            .replace("%(ext)s", ext)
    }
}
