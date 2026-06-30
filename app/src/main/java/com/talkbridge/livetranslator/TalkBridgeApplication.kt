package com.talkbridge.livetranslator

import android.app.Application
import com.talkbridge.livetranslator.data.AppContainer
import com.talkbridge.livetranslator.data.AppDataContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TalkBridgeApplication: Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
        observeClientSettings()
        container.connectivityObserver
        container.bleConnectManager.bind()
    }

    override fun onTerminate() {
        super.onTerminate()
        container.bleConnectManager.unbind()
    }//wird fast nie aufgerufen

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private fun observeClientSettings() {
        applicationScope.launch {
            container.userPreferencesRepository.customIPAddress.collect { ip ->
                container.talkBridgeClient.updateServerAddress(ip)
            }
        }
        applicationScope.launch {
            container.userPreferencesRepository.useBetterTranslation.collect { value ->
                container.talkBridgeClient.setUseBetterTranslation(value)
            }
        }
    }
}