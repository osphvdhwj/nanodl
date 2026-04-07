package com.yourname.nanodl.ui

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import com.yourname.nanodl.R
import com.yourname.nanodl.utils.ConfigManager

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val config = ConfigManager(this)
        val templateInput = findViewById<EditText>(R.id.templateInput)
        val subSwitch = findViewById<Switch>(R.id.subSwitch)
        val saveBtn = findViewById<Button>(R.id.saveBtn)

        templateInput.setText(config.outputTemplate)
        subSwitch.isChecked = config.downloadSubtitles

        saveBtn.setOnClickListener {
            config.outputTemplate = templateInput.text.toString()
            config.downloadSubtitles = subSwitch.isChecked
            Toast.makeText(this, "Configurations Saved!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
