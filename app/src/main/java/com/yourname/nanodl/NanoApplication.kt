package com.yourname.nanodl

import android.app.Application
import com.yourname.nanodl.extractor.NativeDownloader
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.Localization

class NanoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Provide standard localization to appease YouTube's bot checks
        NewPipe.init(NativeDownloader.getInstance(), Localization("en", "US"))
    }
}
