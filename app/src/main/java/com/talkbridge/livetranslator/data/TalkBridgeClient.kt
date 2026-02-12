package com.talkbridge.livetranslator.data

//import com.google.firebase.database.tubesock.WebSocket

class TalkBridgeClient(
    private val serverUrl: String = "ws://192.168.1.100:8000/ws/translate"
) {
//    private var webSocket: WebSocket? = null
//    private val client = OkHttpClient()
//
//
//    fun connect(sourceLang: String, targetLang: String) {
//        val request = Request.Builder()
//            .url(serverUrl)
//            .build()
//
//        webSocket = client.newWebSocket(request, object : WebSocketListener() {
//            override fun onOpen(webSocket: WebSocket, response: Response) {
//                println("WebSocket connected")
//
//                // Initialisierung senden
//                val initData = JSONObject().apply {
//                    put("source_lang", sourceLang)
//                    put("target_lang", targetLang)
//                }
//                webSocket.send(initData.toString())
//            }
//
//            override fun onMessage(webSocket: WebSocket, text: String) {
//                handleJsonMessage(text)
//            }
//
//            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
//                // Audio-Daten empfangen (übersetzte Sprache)
//                playAudio(bytes.toByteArray())
//            }
//
//            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
//                println("WebSocket closing: $reason")
//                stopRecording()
//            }
//
//            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
//                println("WebSocket error: ${t.message}")
//                onError?.invoke(t.message ?: "Unknown error")
//            }
//        })
//    }
//
//
//    private fun handleJsonMessage(message: String) {
//        try {
//            val json = JSONObject(message)
//            val type = json.getString("type")
//
//            when (type) {
//                "ready" -> {
//                    println("Server ready, starting audio recording")
//                    startRecording()
//                }
//                "partial" -> {
//                    val text = json.getString("text")
//                    onPartialResult?.invoke(text)
//                }
//                "final" -> {
//                    val text = json.getString("text")
//                    onFinalResult?.invoke(text)
//                }
//                "translation" -> {
//                    val text = json.getString("text")
//                    onTranslation?.invoke(text)
//                }
//                "state_change" -> {
//                    val state = json.getString("state")
//                    onStateChanged?.invoke(state)
//                }
//            }
//        } catch (e: Exception) {
//            println("Error parsing message: ${e.message}")
//        }
//    }
//
//
//
//    fun disconnect() {
//        webSocket?.close(1000, "Client disconnecting")
//    }
}