package com.yourname.nanodl.utils

import java.io.DataOutputStream
import java.io.File

object RootHelper {
    fun hasRootAccess(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun extractChromeCookies(cacheDir: File): String? {
        if (!hasRootAccess()) return null

        val destFile = File(cacheDir, "nano_chrome_cookies.db")
        val copyCmd = "cp /data/data/com.android.chrome/app_chrome/Default/Cookies ${destFile.absolutePath}"
        val permCmd = "chmod 666 ${destFile.absolutePath}"

        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("$copyCmd\n")
            os.writeBytes("$permCmd\n")
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor()

            if (destFile.exists() && destFile.length() > 0) destFile.absolutePath else null
        } catch (e: Exception) {
            null
        }
    }
}
