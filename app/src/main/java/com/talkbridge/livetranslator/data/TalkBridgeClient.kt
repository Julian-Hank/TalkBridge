package com.talkbridge.livetranslator.data

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.talkbridge.livetranslator.data.audio.AudioOutputManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val TAG: String = "TalkBridgeClient"
private const val DEFAULT_IP = "192.168.68.60"

sealed class ClientEvent {
    object Connected : ClientEvent()
    object Ready : ClientEvent()
    data class LiveTranslationResult(val text: String, val type: SERVER_RESPONSE) : ClientEvent()
    data class TranslationResult(val text: String) : ClientEvent()
    data class TranscriptionResult(val text: String) : ClientEvent()
    data class EstimatedTime(val seconds: Int) : ClientEvent()
    data class LiveTranslationError(val message: String) : ClientEvent()
    object TranscriptionError : ClientEvent()
    object TranslationError : ClientEvent()
}

enum class SERVER_RESPONSE {
    PARTIAL, FINAL, TRANSLATED
}

class TalkBridgeClient(
    private val context: Context,
) {
    var serverIpAddress: String = DEFAULT_IP
        private set

    var useBetterTranslation: Boolean = true
        private set

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)  // kein Timeout für SSE
        .build()

    private val audioOutputManager: AudioOutputManager = AudioOutputManager(context)

    private val _events = MutableSharedFlow<ClientEvent>(extraBufferCapacity = 9)
    val events: SharedFlow<ClientEvent> = _events.asSharedFlow()

    fun updateIpAddress(ip: String) {
        serverIpAddress = ip.ifBlank { DEFAULT_IP }
    }

    fun setUseBetterTranslation(value: Boolean){
        useBetterTranslation = value
//        Log.d(TAG, useBetterTranslation.toString())
    }

    fun connectWebsocket(
        sourceLang: String,
        targetLang: String
    ) {
        val webSocketUrlUrl = "ws://$serverIpAddress:80/ws/translate"

        val request = Request.Builder()
            .url(webSocketUrlUrl)
            .build()

        Log.d(TAG,"trying to connect")
        Log.d(TAG, "useBetterTranslation: $useBetterTranslation")


        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG,"WebSocket connected")

                val initData = JSONObject().apply {
                    put("source_lang", sourceLang)
                    put("target_lang", targetLang)
                    put("use_better_translation", useBetterTranslation)
                }
                webSocket.send(initData.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleJsonMessage(text)
                Log.d(TAG, "Message: $text")
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d(TAG, "bytes: $bytes")
                audioOutputManager.playAudioBytes(bytes.toByteArray())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.d(TAG, "WebSocket error: ${t.message}")
                _events.tryEmit(ClientEvent.LiveTranslationError(t.message ?: "Unknown error"))
            }
        })
    }

    fun sendAudio(audioData: ByteArray){
        if (isAudioOutputBluetooth()){
            webSocket?.send(audioData.toByteString())
        } else {
            if (System.currentTimeMillis() > audioOutputManager.audioFinishTime){
                webSocket?.send(audioData.toByteString())
            }
        }
    }

    fun resetSession() {
        val msg = """{"type":"reset"}"""
        webSocket?.send(msg)
    }

    private fun isAudioOutputBluetooth(): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.isBluetoothA2dpOn
    }

    private fun handleJsonMessage(message: String) {
        try {
            val json = JSONObject(message)
            val type = json.getString("type")

            when (type) {
                "connected" -> {
                    _events.tryEmit(ClientEvent.Connected)
                }
                "ready" -> {
                    _events.tryEmit(ClientEvent.Ready)
                }
                "partial" -> {
                    val text = json.getString("text")
                    _events.tryEmit(ClientEvent.LiveTranslationResult(text, SERVER_RESPONSE.PARTIAL))
                }
                "final" -> {
                    val text = json.getString("text")
                    _events.tryEmit(ClientEvent.LiveTranslationResult(text, SERVER_RESPONSE.FINAL))
                }
                "translated" -> {
                    val text = json.getString("text")
                    _events.tryEmit(ClientEvent.LiveTranslationResult(text, SERVER_RESPONSE.TRANSLATED))
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Error parsing message: ${e.message}")
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnecting")
    }


    fun sendAudioForTranscript(
        audioData: ByteArray,
        lang: String
    ) {
        Log.d(TAG, "Sending audio")
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("lang", lang)
            .addFormDataPart(
                "audio",
                "audio.wav",
                audioData.toRequestBody("audio/wav".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url("http://$serverIpAddress:80/transcript")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                Log.d(TAG, "Response received")
                response.body?.source()?.let { source ->
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        Log.d(TAG, "Line: $line")

                        if (line.startsWith("data: ")) {
                            val json = JSONObject(line.removePrefix("data: "))
                            when (json.getString("type")) {
                                "estimated_time" -> {
                                    val seconds = json.getInt("seconds")
                                    _events.tryEmit(ClientEvent.EstimatedTime(json.getInt("seconds")))
                                }
                                "transcript" -> {
                                    val text = json.getString("text")
                                    _events.tryEmit(ClientEvent.TranscriptionResult(json.getString("text")))
                                }
                            }
                        }
                    }
                }
                Log.d(TAG, "Finished Transcription")
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.d(TAG, "Error: ${e.message}")
                _events.tryEmit(ClientEvent.TranscriptionError)
            }
        })
    }

    fun sendTextForTranslation(
        text: String = "",
        sourceLang: String,
        targetLang: String
    ){
        if (text == ""){
            return
        }
        Log.d(TAG, "Sending Text for translation")
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("text", text)
            .addFormDataPart("source_lang", sourceLang)
            .addFormDataPart("target_lang", targetLang)
            .build()

        val request = Request.Builder()
            .url("http://$serverIpAddress:80/translate")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return
                val translated = JSONObject(body).getString("translated")
                _events.tryEmit(ClientEvent.TranslationResult(translated))
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.d(TAG, "Translation error: ${e.message}")
                _events.tryEmit(ClientEvent.TranslationError)
            }
        })
    }
}