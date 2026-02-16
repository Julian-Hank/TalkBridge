package com.talkbridge.livetranslator

import android.app.Application
import com.talkbridge.livetranslator.data.AppContainer
import com.talkbridge.livetranslator.data.AppDataContainer

class TalkBridgeApplication: Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
    }
}