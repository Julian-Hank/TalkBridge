package com.talkbridge.livetranslator.data

import android.content.Context
import android.util.Log
import com.talkbridge.livetranslator.data.audio.AudioOutputManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONObject

class TalkBridgeClient(
    private val context: Context,
    private val serverUrl: String = "ws://192.168.178.74:8000/ws/translate" //zusätzlich für emulator 10.0.2.2 , für physisch: 192.168.178.74
) {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val audioOutputManager: AudioOutputManager = AudioOutputManager(context)

    var onReady: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onConnected :(() -> Unit)? = null
    var onPartial: ((String) -> Unit)? = null
    var onFinal: ((String) -> Unit)? = null
    var ontranslationResponse: ((String) -> Unit)? = null


    fun connect(sourceLang: String, targetLang: String) {
        val request = Request.Builder()
            .url(serverUrl)
            .build()

        Log.d("TalkBridgeClient","trying to connect")

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("TalkBridgeClient","WebSocket connected")

                val initData = JSONObject().apply {
                    put("source_lang", sourceLang)
                    put("target_lang", targetLang)
                }
                webSocket.send(initData.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleJsonMessage(text)
                Log.d("TalkBridgeClient", "Message: $text")
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // Audio-Daten empfangen (übersetzte Sprache)
                Log.d("TalkBridgeClient", "bytes: $bytes")
                audioOutputManager.playAudioBytes(bytes.toByteArray())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("TalkBridgeClient", "WebSocket closing: $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.d("TalkBridgeClient", "WebSocket error: ${t.message}")
                onError?.invoke(t.message ?: "Unknown error")
            }
        })
    }

    fun sendAudio(audioData: ByteArray){
        webSocket?.send(audioData.toByteString())
    }

    private fun handleJsonMessage(message: String) {
        try {
            val json = JSONObject(message)
            val type = json.getString("type")

            when (type) {
                "connected" -> {
                    onConnected?.invoke()
                }
                "ready" -> {
                    onReady?.invoke()
                }
                "partial" -> {
                    val text = json.getString("text")
                    ontranslationResponse?.invoke(text)
                }
                "final" -> {
                    val text = json.getString("text")
                    ontranslationResponse?.invoke(text)
//                    onFinalResult?.invoke(text)
                }
//                "translation" -> {
//                    val text = json.getString("text")
////                    onTranslation?.invoke(text)
//                }
//                "state_change" -> {
//                    val state = json.getString("state")
////                    onStateChanged?.invoke(state)
//                }
            }
        } catch (e: Exception) {
            Log.d("TalkBridgeClient", "Error parsing message: ${e.message}")
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnecting")
    }
}