package com.talkbridge.livetranslator.data

import android.content.Context
import android.util.Log
import com.talkbridge.livetranslator.data.audio.AudioOutputManager
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
import okhttp3.Callback
import okhttp3.Call
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val TAG: String = "TalkBridgeClient"

class TalkBridgeClient(
    context: Context,
//    private val ipAddress: String = "192.168.178.74:8000" //  ws://192.168.178.74:8000/ws/translate
) {

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)  // kein Timeout für SSE
        .build()

    private val audioOutputManager: AudioOutputManager = AudioOutputManager(context)

    var onReady: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onConnected :(() -> Unit)? = null
    var onTranslationResponse: ((String) -> Unit)? = null
    var onTranscriptionResponse: ((String) -> Unit)? = null
    var onTranscriptionError: (() -> Unit)? = null
    var onEstimatedTimeResult: ((Int) -> Unit)? = null


    fun connectWebsocket(
        ipAddress: String = "192.168.178.74:8000",
        sourceLang: String,
        targetLang: String
    ) {
        val webSocketUrlUrl: String = "ws://$ipAddress/ws/translate"

        val request = Request.Builder()
            .url(webSocketUrlUrl)
            .build()

        Log.d(TAG,"trying to connect")

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG,"WebSocket connected")

                val initData = JSONObject().apply {
                    put("source_lang", sourceLang)
                    put("target_lang", targetLang)
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
                    onTranslationResponse?.invoke(text)
                }
                "final" -> {
                    val text = json.getString("text")
                    onTranslationResponse?.invoke(text)
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
        ipAddress: String = "192.168.178.74:8000",
        audioData: ByteArray, lang: String
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
            .url("http://$ipAddress/transcript")
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
                                    onEstimatedTimeResult?.invoke(seconds)
                                }
                                "transcript" -> {
                                    val text = json.getString("text")
                                    onTranscriptionResponse?.invoke(text)
                                }
                            }
                        }
                    }
                }
                Log.d(TAG, "Finished Transcription")
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.d(TAG, "Error: ${e.message}")
                onTranscriptionError?.invoke()
            }
        })
    }
}