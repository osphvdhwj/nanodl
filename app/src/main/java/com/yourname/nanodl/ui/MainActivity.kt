package com.yourname.nanodl.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.yourname.nanodl.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fabPasteLink = findViewById<ExtendedFloatingActionButton>(R.id.fabPasteLink)

        fabPasteLink.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboard.hasPrimaryClip() && clipboard.primaryClip?.itemCount ?: 0 > 0) {
                val pasteData = clipboard.primaryClip?.getItemAt(0)?.text.toString()
                
                if (pasteData.contains("http")) {
                    val intent = Intent(this, InterceptorActivity::class.java).apply {
                        putExtra(Intent.EXTRA_TEXT, pasteData)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "No valid URL in clipboard", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
