package com.talkbridge.livetranslator.ui.translate

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talkbridge.livetranslator.R
import com.talkbridge.livetranslator.TalkBridgeBottomNavBar
import com.talkbridge.livetranslator.TalkBridgeTopAppBar
import com.talkbridge.livetranslator.data.Language
import com.talkbridge.livetranslator.data.LanguageData
import com.talkbridge.livetranslator.data.LanguageDataSource
import com.talkbridge.livetranslator.ui.navigation.NavigationDestinationWithIcon
import com.talkbridge.livetranslator.ui.theme.TalkBridgeLiveTheme
import com.talkbridge.livetranslator.ui.theme.primary
import com.talkbridge.livetranslator.ui.theme.secondary
import com.talkbridge.livetranslator.ui.theme.tertiary

object TranslateDestination : NavigationDestinationWithIcon {
    override val route = "translate"
    override val titleRes = R.string.translate
    override val icon = R.drawable.translate
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateScreen(
    modifier: Modifier = Modifier,
    onNavigationButtonClick: (NavigationDestinationWithIcon) -> Unit = {},
    openSettings: () -> Unit,
    onTargetLanguageClick: () -> Unit,
    onSourceLanguageClick: () -> Unit,
    onLanguageSwapClick: () -> Unit,
    onInputChanged: (String) -> Unit,
    uiState: TranslateUiState
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TalkBridgeTopAppBar(
                title = stringResource(TranslateDestination.titleRes),
                canNavigateBack = false,
                openSettings = openSettings
            )
        },
        bottomBar = {
            TalkBridgeBottomNavBar(
                onNavigationButtonClick = onNavigationButtonClick,
                currentRoute = TranslateDestination.route
            )
        }
    ) { innerPadding ->
        TranslateBody(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            onTargetLanguageClick = onTargetLanguageClick,
            onSourceLanguageClick = onSourceLanguageClick,
            onInputChanged = onInputChanged,
            onLanguageSwapClick = onLanguageSwapClick,
            uiState = uiState
        )
    }
}

@Composable
fun TranslateBody(
    onTargetLanguageClick: () -> Unit = {},
    onSourceLanguageClick: () -> Unit = {},
    onInputChanged: (String) -> Unit,
    onLanguageSwapClick: () -> Unit,
    uiState: TranslateUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(top = 12.dp)
    ) {
        LanguageSwapCard(
            sourceLanguage = uiState.sourceLanguage,
            targetLanguage = uiState.targetLanguage,
            onTargetLanguageClick = onTargetLanguageClick,
            onSourceLanguageClick = onSourceLanguageClick,
            onSwapClick = onLanguageSwapClick
        )
        Spacer(modifier = Modifier.height(20.dp))
        TranslateContainer(
            inputText = uiState.sourceLanguageText ?: "",
            translatedText = uiState.targetLanguageText ?: "",
            onInputChanged = onInputChanged
        )
    }
}

@Composable
fun TranslateContainer(
    inputText: String = "",
    translatedText: String = "",
    onInputChanged: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val minSectionHeight = 140.dp
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = minSectionHeight)
                    .padding(16.dp)
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = onInputChanged,
                    modifier = Modifier
                        .padding(top = 8.dp),
                    textStyle = MaterialTheme.typography.displayMedium.copy(
                        color = primary
                    ),
                    decorationBox = { innerTextField ->
                        Box {
                            if (inputText.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.enter_text),
                                    style = MaterialTheme.typography.displayMedium,
                                    color = primary.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            HorizontalDivider(
                color = secondary,
                thickness = 1.dp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = minSectionHeight),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    BasicTextField(
                        value = translatedText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .padding(top = 8.dp),
                        textStyle = MaterialTheme.typography.displayMedium.copy(
                            color = primary
                        ),
                        decorationBox = { innerTextField ->
                            Box {
                                if (translatedText.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.translation),
                                        style = MaterialTheme.typography.displayMedium,
                                        color = primary.copy(alpha = 0.5f)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
                if (translatedText != ""){
                    val toastText = stringResource(R.string.copied)
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(translatedText))
                            Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.content_copy),
                            contentDescription = stringResource(R.string.copy),
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageSwapCard(
    sourceLanguage: LanguageData = LanguageDataSource.languagesMap.getValue(Language.GERMAN),
    targetLanguage: LanguageData = LanguageDataSource.languagesMap.getValue(Language.FRENCH),
    onSwapClick: () -> Unit = {},
    onTargetLanguageClick: () -> Unit = {},
    onSourceLanguageClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = tertiary
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageItem(
                    language = sourceLanguage.languageName,
                    flagRes = sourceLanguage.flag,
                    onClick = onSourceLanguageClick,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.weight(1f))

                LanguageItem(
                    language = targetLanguage.languageName,
                    flagRes = targetLanguage.flag,
                    onClick =  onTargetLanguageClick,
                    modifier = Modifier.weight(1f),
                    isLeftHandSide = true
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
//                    .background(
//                        color = onTertiary,
//                        shape = CircleShape
//                    )
                    .clickable { onSwapClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    //imageVector = Icons.Filled.ArrowForward,
                    painter = painterResource(R.drawable.swap_icon),
                    contentDescription = stringResource(R.string.switch_languages),
                    tint = primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

}


@Composable
fun LanguageItem(
    @StringRes language: Int,
    @DrawableRes flagRes: Int,
    onClick: () -> Unit,
    isLeftHandSide: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box {
        Row (
            modifier = modifier.clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isLeftHandSide){
                Image(
                    painter = painterResource(flagRes),
                    contentDescription = stringResource(language),
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.widthIn(6.dp))

                Text(
                    text = stringResource(language),
                    color = primary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = stringResource(language),
                    color = primary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.widthIn(6.dp))

                Image(
                    painter = painterResource(flagRes),
                    contentDescription = stringResource(language),
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Preview
@Composable
private fun LanguageCardPreview() {
    TalkBridgeLiveTheme {
        LanguageSwapCard()
    }
}