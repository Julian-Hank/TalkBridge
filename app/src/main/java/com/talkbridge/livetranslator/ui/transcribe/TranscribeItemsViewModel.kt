package com.talkbridge.livetranslator.ui.transcribe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talkbridge.livetranslator.data.local.entity.TranscriptionItem
import com.talkbridge.livetranslator.data.repository.TranscriptionItemsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TranscribeItemsViewModel(val transcriptionItemsRepository: TranscriptionItemsRepository) : ViewModel() {

    val transcriptionItemsUiState: StateFlow<TranscriptionItemsUiState> =
        transcriptionItemsRepository.getAllItemsStream().map { TranscriptionItemsUiState(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                initialValue = TranscriptionItemsUiState()
            )

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }

    fun deleteItem(id: Long){
        viewModelScope.launch {
            val item = transcriptionItemsRepository.getItem(id)
            if (item != null){
                transcriptionItemsRepository.deleteItem(item)
            }
        }
    }
}

data class TranscriptionItemsUiState(
    val transcriptionItemsList: List<TranscriptionItem> = listOf()
)