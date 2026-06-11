package com.talkbridge.livetranslator.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.talkbridge.livetranslator.MainActivity
import com.talkbridge.livetranslator.R
import com.talkbridge.livetranslator.TalkBridgeApplication
import com.talkbridge.livetranslator.data.audio.AudioRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TalkBridgeForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "talkbridge_channel"
        const val NOTIFICATION_ID = 1

        const val ACTION_START_LIVE = "ACTION_START_LIVE"
        const val ACTION_PAUSE_LIVE = "ACTION_PAUSE_LIVE"
        const val ACTION_RESUME_LIVE = "ACTION_RESUME_LIVE"

        const val ACTION_START_TRANSCRIBE = "ACTION_START_TRANSCRIBE"
        const val ACTION_PAUSE_TRANSCRIBE = "ACTION_PAUSE_TRANSCRIBE"
        const val ACTION_RESUME_TRANSCRIBE = "ACTION_RESUME_TRANSCRIBE"

        const val ACTION_STOP = "ACTION_STOP"

        const val EXTRA_AUDIO_DATA = "extra_audio_data"
        const val EXTRA_TRANSCRIBE_LANG = "extra_transcribe_lang"

        fun pauseLiveIntent(context: Context) =
            Intent(context, TalkBridgeForegroundService::class.java).apply {
                action = ACTION_PAUSE_LIVE
            }

        fun resumeLiveIntent(context: Context) =
            Intent(context, TalkBridgeForegroundService::class.java).apply {
                action = ACTION_RESUME_LIVE
            }

        fun startLiveIntent(context: Context) =
            Intent(context, TalkBridgeForegroundService::class.java).apply {
                action = ACTION_START_LIVE
            }

        fun startTranscribeIntent(context: Context) =
            Intent(context, TalkBridgeForegroundService::class.java).apply {
                action = ACTION_START_TRANSCRIBE
            }

        fun pauseTranscribeIntent(context: Context) =
            Intent(context, TalkBridgeForegroundService::class.java).apply {
                action = ACTION_PAUSE_TRANSCRIBE
            }

        fun resumeTranscribeIntent(context: Context) =
            Intent(context, TalkBridgeForegroundService::class.java).apply {
                action = ACTION_RESUME_TRANSCRIBE
            }

        fun stopIntent(context: Context) =
            Intent(context, TalkBridgeForegroundService::class.java).apply {
                action = ACTION_STOP
            }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var client: TalkBridgeClient
    private lateinit var container: AppContainer
    private val audioRecorder = AudioRecorder()
    private var currentMode: String? = null

    override fun onCreate() {
        super.onCreate()
        container = (application as TalkBridgeApplication).container
        client = container.talkBridgeClient
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LIVE -> {
                currentMode = ACTION_START_LIVE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        buildNotification("Live-Übersetzung läuft..."),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, buildNotification("Live-Übersetzung läuft..."))
                }
                startLiveTranslationRecording()
            }
            ACTION_PAUSE_LIVE -> {
                audioRecorder.stopRecording()
                client.resetSession()
                updateNotification("Live-Übersetzung pausiert")
            }
            ACTION_RESUME_LIVE -> {
                startLiveTranslationRecording()
                updateNotification("Live-Übersetzung läuft...")
            }
            ACTION_START_TRANSCRIBE -> {
                currentMode = ACTION_START_TRANSCRIBE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        buildNotification("Transktiptionsaufnahme läuft..."),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, buildNotification("Transktiptionsaufnahme läuft..."))
                }
                startTranscription()
            }
            ACTION_PAUSE_TRANSCRIBE -> {
                audioRecorder.stopRecording()
                updateNotification("Transktiptionsaufnahme pausiert")
            }
            ACTION_RESUME_TRANSCRIBE -> {
                startTranscription()
                updateNotification("Transktiptionsaufnahme läuft...")
            }
//            ACTION_START_TRANSCRIBE -> {
//                val audio = intent.getByteArrayExtra(EXTRA_AUDIO_DATA) ?: return START_NOT_STICKY
//                val lang = intent.getStringExtra(EXTRA_TRANSCRIBE_LANG) ?: "auto"
//                currentMode = ACTION_START_TRANSCRIBE
//                startForeground(NOTIFICATION_ID, buildNotification("Transkription läuft..."))
//                startTranscription(audio, lang)
//            }
            ACTION_STOP -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startLiveTranslationRecording() {
        serviceScope.launch {
            audioRecorder.startRecording { audioData ->
                client.sendAudio(audioData)
            }
        }
    }

    private fun startTranscription(){
        serviceScope.launch {
            audioRecorder.startRecording { audioData ->
                container.transcribeRecordingAudioFlow.tryEmit(audioData)
            }
        }
    }

    private fun sendTranscriptionAudio(audioData: ByteArray, lang: String) {
        serviceScope.launch {
            client.sendAudioForTranscript(audioData, lang)
            // Service stoppt sich selbst, nachdem das Ergebnis versendet wurde.
            // Das ViewModel empfängt es über den SharedFlow.
            stopSelf()
        }
    }

    override fun onDestroy() {
        audioRecorder.stopRecording()
        if (currentMode == ACTION_START_LIVE) {
            client.disconnect()
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf() // löst onDestroy aus -> disconnect -> Disconnected Event
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Notification ──────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "TalkBridge",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Hintergrundübersetzung aktiv"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(contentText: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setOngoing(true)
            .setContentTitle("TalkBridge")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openIntent)
            .build()
            .also { it.flags = it.flags or Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT }
    }

    private fun updateNotification(contentText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(contentText))
    }
}