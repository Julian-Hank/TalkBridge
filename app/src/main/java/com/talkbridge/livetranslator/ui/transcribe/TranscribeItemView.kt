package com.talkbridge.livetranslator.ui.transcribe

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.talkbridge.livetranslator.R
import com.talkbridge.livetranslator.TalkBridgeTopAppBar
import com.talkbridge.livetranslator.ui.navigation.NavigationDestination

object TranscribeItemViewDestination : NavigationDestination {
    override val route = "transcribe_item_view"
    override val titleRes = R.string.language_select
    const val transcribeItemArg = "id"
    val routeWithArgs = "$route/{$transcribeItemArg}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscribeItemViewScreen(
    viewModel: TranscribeItemViewModel,
    onBackButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.transcriptionItemUiState.collectAsState()

    Scaffold(
        topBar = {
            TalkBridgeTopAppBar(
                title = uiState.transcriptionItem?.title,
                canNavigateBack = true,
                navigateUp = onBackButtonClick
            )
        },
    ) { innerPadding ->
        SelectionContainer {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
            ) {
                item{
                    Text(
                        text = uiState.transcriptionItem?.date.toString(),
                    )
                }
                item{
                    Text(
                        text = uiState.transcriptionItem?.content ?: "",
                    )
                }
                // Text "Absperrung"?
            }
        }
    }
}