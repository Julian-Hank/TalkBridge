package com.talkbridge.livetranslator.data.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudioRecorder(
//    private val audioOutputManager: AudioOutputManager
) {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    private val sampleRate = 16000 // 16kHz
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        channelConfig,
        audioFormat
    )

    suspend fun startRecording(onAudioData: (ByteArray) -> Unit) = withContext(Dispatchers.IO) {
        try {

//            audioOutputManager.playRecordingStartSound()
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (AcousticEchoCanceler.isAvailable()) {
                AcousticEchoCanceler.create(audioRecord!!.audioSessionId)?.enabled = true
            }

            audioRecord?.startRecording()
            isRecording = true

            val buffer = ByteArray(bufferSize)

            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    onAudioData(buffer.copyOf(read))
                }
            }
        } catch (e: SecurityException) {
            // Permission fehlt
            e.printStackTrace()
        }
    }

    fun stopRecording() {
        if (isRecording){
            isRecording = false
            audioRecord?.apply {
                stop()
                release()
            }
            audioRecord = null
        } else {
            Log.w("AudioRecorder", "nothing to stop")
        }
    }
}