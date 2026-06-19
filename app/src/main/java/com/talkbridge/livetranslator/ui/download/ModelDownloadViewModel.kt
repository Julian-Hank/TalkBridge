package com.talkbridge.livetranslator.ui.download

import androidx.lifecycle.ViewModel

class ModelDownloadViewModel: ViewModel() {

}

data class DownloadableModel(
    val name: String,
    val description: String,
    val size: Float
)

val DownloadableModels = listOf<DownloadableModel>(
    DownloadableModel(
        "NLLB",
        "No Language Left Behind (NLLB) by Meta is a translation model capable of translating into 200 different languages.\n" +
                "It is used as the offline translation engine",
        2.3f),
    DownloadableModel(
        "Openai Whisper",
        "...",
        0f),
)