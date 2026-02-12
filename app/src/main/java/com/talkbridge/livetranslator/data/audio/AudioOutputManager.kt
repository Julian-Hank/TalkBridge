package com.talkbridge.livetranslator.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import com.talkbridge.livetranslator.R

class AudioOutputManager(private val context: Context) {

    // Für kurze System-Sounds (Start/Stop/Error)
    private var soundPool: SoundPool? = null
    private var startSoundId: Int = -1
    private var stopSoundId: Int = -1
    private var errorSoundId: Int = -1

    // Für längere Audio-Wiedergabe (TTS vom Backend)
    private var mediaPlayer: MediaPlayer? = null

    init {
        initializeSoundPool()
    }

    private fun initializeSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        // Custom Sounds aus res/raw laden
        startSoundId = soundPool?.load(context, R.raw.recording_start, 1) ?: -1
//        stopSoundId = soundPool?.load(context, R.raw.recording_stop, 1) ?: -1
//        errorSoundId = soundPool?.load(context, R.raw.error_sound, 1) ?: -1

    }


    /**
     * Spielt einen Sound beim Start der Aufnahme
     */
    fun playRecordingStartSound() {
        try {
            soundPool?.play(startSoundId, 1f, 1f, 1, 0, 1f)
        } catch (e: Exception) {
            Log.e("AudioOutputManager", "Error playing start sound", e)
        }
    }

    /**
     * Spielt einen Sound beim Stoppen der Aufnahme
     */
    fun playRecordingStopSound() {
        try {
            soundPool?.play(stopSoundId, 1f, 1f, 1, 0, 1f)
        } catch (e: Exception) {
            Log.e("AudioOutputManager", "Error playing stop sound", e)
        }
    }

    /**
     * Spielt einen Error-Sound
     */
    fun playErrorSound() {
        try {
            soundPool?.play(errorSoundId, 1f, 1f, 1, 0, 1f)
        } catch (e: Exception) {
            Log.e("AudioOutputManager", "Error playing error sound", e)
        }
    }
}