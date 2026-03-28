package com.talkbridge.livetranslator.ui.transcribe


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talkbridge.livetranslator.R
import com.talkbridge.livetranslator.TalkBridgeTopAppBar
import com.talkbridge.livetranslator.ui.navigation.NavigationDestination
import com.talkbridge.livetranslator.ui.theme.TalkBridgeLiveTheme
import com.talkbridge.livetranslator.ui.theme.error
import com.talkbridge.livetranslator.ui.theme.primary
import com.talkbridge.livetranslator.ui.theme.secondary
import com.talkbridge.livetranslator.ui.theme.tertiary


object TranscribeItemsOverviewDestination : NavigationDestination {
    override val route = "transcribeItemsOverview"
    override val titleRes = R.string.transcriptions
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscribeItemsScreen(
    uiState: TranscriptionItemsUiState,
    onBackButtonClick: () -> Unit,
    navigateToTranscription: (Long) -> Unit,
    deleteItem: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TalkBridgeTopAppBar(
                title = stringResource(TranscribeItemsOverviewDestination.titleRes),
                canNavigateBack = true,
                navigateUp = onBackButtonClick
            )
        },
    ) { innerPadding ->
        TranscribeItemsBody(
            uiState = uiState,
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            navigateToTranscription = navigateToTranscription,
            deleteItem = deleteItem
        )
    }
}

@Composable
fun TranscribeItemsBody(
    uiState: TranscriptionItemsUiState,
    navigateToTranscription: (Long) -> Unit,
    deleteItem: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn (modifier = modifier) {
        if (uiState.transcriptionItemsList.isEmpty()){
            item{
                Text(text = stringResource(R.string.no_transcriptions))
            }
        } else {
            items(items = uiState.transcriptionItemsList) { item ->
                TranscriptionItemCard(
                    id = item.id,
                    title = item.title,
                    dateString = item.date.toString(),
                    navigateToTranscription = navigateToTranscription,
                    deleteItem = deleteItem
                )
            }
        }
    }
}

@Composable
fun TranscriptionItemCard(
    id: Long,
    title: String,
    dateString: String,
    navigateToTranscription: (Long) -> Unit,
    deleteItem: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
//            .height(20.dp)
            .clickable(onClick = { navigateToTranscription(id) }),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = tertiary
        ),
    ){
        Row (
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Column (
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = primary,
                    modifier = Modifier
                        .clickable(onClick = {})
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondary,
                    fontSize = 12.sp
                )
            }

            var showDialog by remember { mutableStateOf(false) }

            if (showDialog) {
                DeleteDialog(
                    onConfirm = {
                        deleteItem(id)
                        showDialog = false
                    },
                    onDismiss = { showDialog = false }
                )
            }

            IconButton(
                modifier = Modifier
                    .wrapContentWidth(align = Alignment.End),
                onClick = { showDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = primary
                )
            }
        }
    }
}

@Composable
fun DeleteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.delete_entry)) },
        text = { Text(text = stringResource(R.string.sure_delete_entry)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.delete), color = error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}

@Preview
@Composable
private fun TranscriptionItemCardPreview() {
    TalkBridgeLiveTheme {
        TranscriptionItemCard(
            0L, "Title", "date", {}, {}
        )
    }
}