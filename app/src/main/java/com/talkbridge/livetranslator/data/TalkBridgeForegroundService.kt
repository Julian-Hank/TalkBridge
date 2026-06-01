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
        const val ACTION_START_TRANSCRIBE = "ACTION_START_TRANSCRIBE"
        const val ACTION_STOP = "ACTION_STOP"

        const val EXTRA_AUDIO_DATA = "extra_audio_data"
        const val EXTRA_TRANSCRIBE_LANG = "extra_transcribe_lang"

        const val ACTION_PAUSE_LIVE = "ACTION_PAUSE"
        const val ACTION_RESUME_LIVE = "ACTION_RESUME"

        fun pauseIntent(context: Context) =
            Intent(context, TalkBridgeForegroundService::class.java).apply {
                action = ACTION_PAUSE_LIVE
            }

        fun resumeIntent(context: Context) =
            Intent(context, TalkBridgeForegroundService::class.java).apply {
                action = ACTION_RESUME_LIVE
            }

        fun startLiveIntent(context: Context) =
            Intent(context, TalkBridgeForegroundService::class.java).apply {
                action = ACTION_START_LIVE
            }

        fun startTranscribeIntent(context: Context, audioData: ByteArray, lang: String) =
            Intent(context, TalkBridgeForegroundService::class.java).apply {
                action = ACTION_START_TRANSCRIBE
                putExtra(EXTRA_AUDIO_DATA, audioData)
                putExtra(EXTRA_TRANSCRIBE_LANG, lang)
            }

        fun stopIntent(context: Context) =
            Intent(context, TalkBridgeForegroundService::class.java).apply {
                action = ACTION_STOP
            }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var client: TalkBridgeClient
    private val audioRecorder = AudioRecorder()
    private var currentMode: String? = null

    override fun onCreate() {
        super.onCreate()
        client = (application as TalkBridgeApplication).container.talkBridgeClient
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
                serviceScope.launch {
                    audioRecorder.startRecording { audioData ->
                        client.sendAudio(audioData)
                    }
                }
                updateNotification("Live-Übersetzung läuft...")
            }
//            ACTION_START_TRANSCRIBE -> {
//                val audio = intent.getByteArrayExtra(EXTRA_AUDIO_DATA) ?: return START_NOT_STICKY
//                val lang = intent.getStringExtra(EXTRA_TRANSCRIBE_LANG) ?: "auto"
//                currentMode = ACTION_START_TRANSCRIBE
//                startForeground(NOTIFICATION_ID, buildNotification("Transkription läuft..."))
//                startTranscription(audio, lang)
//            }
//            ACTION_STOP -> stopSelf()
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

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Notification ──────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "TalkBridge",
            NotificationManager.IMPORTANCE_LOW
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
            .setContentTitle("TalkBridge")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(contentText))
    }

//    private fun buildNotification(contentText: String): Notification {
//        val openIntent = PendingIntent.getActivity(
//            this, 0,
//            Intent(this, MainActivity::class.java).apply {
//                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
//            },
//            PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val stopIntent = PendingIntent.getService(
//            this, 1,
//            stopIntent(this),
//            PendingIntent.FLAG_IMMUTABLE
//        )
//
//        return NotificationCompat.Builder(this, CHANNEL_ID)
//            .setContentTitle("TalkBridge")
//            .setContentText(contentText)
//            .setSmallIcon(R.drawable.ic_launcher_foreground)
//            .setContentIntent(openIntent)
//            .addAction(0, "Stoppen", stopIntent)
//            .setOngoing(true)
//            .build()
//    }
}