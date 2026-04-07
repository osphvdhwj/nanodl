package com.yourname.nanodl.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.yourname.nanodl.R
import com.yourname.nanodl.utils.ConfigManager

class MainActivity : Activity() {
    private lateinit var config: ConfigManager
    private lateinit var currentLocationTxt: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        config = ConfigManager(this)

        currentLocationTxt = findViewById(R.id.currentLocationTxt)
        val templateInput = findViewById<EditText>(R.id.templateInput)
        val subSwitch = findViewById<Switch>(R.id.subSwitch)
        val saveBtn = findViewById<Button>(R.id.saveBtn)
        val btnSelectFolder = findViewById<Button>(R.id.btnSelectFolder)

        // Request Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Load current configurations
        templateInput.setText(config.outputTemplate)
        subSwitch.isChecked = config.downloadSubtitles
        updateStorageUI()

        // Storage Picker Setup
        btnSelectFolder.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, 9999)
        }

        saveBtn.setOnClickListener {
            config.outputTemplate = templateInput.text.toString()
            config.downloadSubtitles = subSwitch.isChecked
            Toast.makeText(this, "Configurations Saved!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 9999 && resultCode == RESULT_OK) {
            val uri: Uri? = data?.data
            if (uri != null) {
                // Save the persistent permission so we can write here later without asking again
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                config.customStorageUri = uri.toString()
                updateStorageUI()
            }
        }
    }

    private fun updateStorageUI() {
        val uriStr = config.customStorageUri
        if (uriStr != null) {
            val uri = Uri.parse(uriStr)
            val path = uri.path?.split(":")?.lastOrNull() ?: "Custom Folder"
            currentLocationTxt.text = "Saving to: $path"
        } else {
            currentLocationTxt.text = "Default: Device Movies Folder"
        }
    }
}
