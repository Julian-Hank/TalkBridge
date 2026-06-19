package com.talkbridge.livetranslator.ui.download

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talkbridge.livetranslator.R
import com.talkbridge.livetranslator.TalkBridgeTopAppBar
import com.talkbridge.livetranslator.ui.navigation.NavigationDestination
import com.talkbridge.livetranslator.ui.theme.primary


object ModelDownLoadDestination : NavigationDestination {
    override val route = "model_downLoad"
    override val titleRes = R.string.model_download
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDownLoadScreen(
    onBackButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TalkBridgeTopAppBar(
                title = stringResource(ModelDownLoadDestination.titleRes),
                canNavigateBack = true,
                navigateUp = onBackButtonClick
            )
        },
    ) { innerPadding ->
        ModelDownloadBody(modifier = Modifier.padding(innerPadding))
    }
}


@Composable
fun ModelDownloadBody(modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
        item {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                text = "Here you can download the Models used for Translation and Transcription, " +
                        "so the app can be used offline",
                fontSize = 16.sp
            )
        }
        item {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
        items(
            items = DownloadableModels,
        ) { downloadableModel ->
            ModelDownloadItem(downloadableModel = downloadableModel)
        }
    }
}

@Composable
fun ModelDownloadItem(modifier: Modifier = Modifier, downloadableModel: DownloadableModel) {
    Box(
        modifier = modifier
            .padding(top = 4.dp)
            .fillMaxWidth(),

    ){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
            Column(
                modifier = Modifier.weight(7f)
            ) {
                Text(
                    text = downloadableModel.name,
                    fontSize = 20.sp,
                    color = primary,
                )
                Text(
                    text = downloadableModel.description,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    text = "~${downloadableModel.size} GB",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
            IconButton(
                onClick = {},
                modifier = Modifier.weight(1.25f)
            ) {
                Icon(
                    painter = painterResource(R.drawable.outline_download_24),//download img in the future
                    contentDescription = null,
                    tint = primary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ModelDownloadItemPreview() {
    ModelDownloadItem(
        downloadableModel = DownloadableModels[0]
    )
}

@Preview(showBackground = true)
@Composable
private fun ModelDownloadScreenPreview() {
    ModelDownLoadScreen({})
}