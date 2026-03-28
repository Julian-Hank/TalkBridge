package com.talkbridge.livetranslator.ui.transcribe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talkbridge.livetranslator.data.local.entity.TranscriptionItem
import com.talkbridge.livetranslator.data.repository.TranscriptionItemsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class TranscribeItemViewModel(
    savedStateHandle: SavedStateHandle,
    transcriptionItemsRepository: TranscriptionItemsRepository
) : ViewModel() {

    private val transcriptionItemId: Long = checkNotNull(savedStateHandle["id"])

    val transcriptionItemUiState: StateFlow<TranscriptionItemUiState> =
        transcriptionItemsRepository.getItemStream(transcriptionItemId).map { TranscriptionItemUiState(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                initialValue = TranscriptionItemUiState()
            )

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

data class TranscriptionItemUiState(
    var transcriptionItem: TranscriptionItem? = null
)