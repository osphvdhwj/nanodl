package com.yourname.nanodl

import android.app.Application
import com.yourname.nanodl.extractor.NativeDownloader
import org.schabi.newpipe.extractor.NewPipe

class NanoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize the extractor brain with our tiny downloader
        NewPipe.init(NativeDownloader.getInstance())
    }
}