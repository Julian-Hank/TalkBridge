package com.talkbridge.livetranslator.ui.common


import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.talkbridge.livetranslator.R
import com.talkbridge.livetranslator.TalkBridgeTopAppBar
import com.talkbridge.livetranslator.data.Language
import com.talkbridge.livetranslator.data.LanguageData
import com.talkbridge.livetranslator.data.LanguageDataSource
import com.talkbridge.livetranslator.ui.navigation.NavigationDestination
import com.talkbridge.livetranslator.ui.theme.TalkBridgeLiveTheme
import com.talkbridge.livetranslator.ui.theme.primary
import com.talkbridge.livetranslator.ui.theme.secondary
import com.talkbridge.livetranslator.ui.theme.tertiary

object LanguageSelectDestination : NavigationDestination {
    override val route = "language_select"
    override val titleRes = R.string.language_select
    const val languageTypeArg = "languageType"
    val routeWithArgs = "$route/{$languageTypeArg}"
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectScreen(
    modifier: Modifier = Modifier,
    languageType: String, // "source" oder "target"
    onBackButtonClick: () -> Unit,
    onLanguageSelected: (LanguageData) -> Unit
){
    Scaffold(
        topBar = {
            TalkBridgeTopAppBar(
                title = stringResource(if (languageType == "source") R.string.source_language else R.string.target_language),
                canNavigateBack = true,
                navigateUp = onBackButtonClick
            )
        },
    ) { innerPadding ->
        LanguageSelectBody(
            modifier = modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            onLanguageSelected = onLanguageSelected
        )
    }
}

@Composable
fun LanguageSelectBody(
    modifier: Modifier = Modifier,
    onLanguageSelected: (LanguageData) -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val allLanguages = LanguageDataSource.languagesList
    val recentLanguages = listOf(
        LanguageData(R.string.german, R.drawable.germany_flag_circular),
        LanguageData(R.string.english, R.drawable.uk_flag_circular),
    )

    val filteredLanguages = remember(query) {
        if (query.isEmpty()) {
            allLanguages
        } else {
            allLanguages.filter { language ->
                val languageName = context.getString(language.languageName)
                languageName.contains(query, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize()
    ) {
        // SearchBar
        item {
            LanguageSearchBar(
                query = query,
                onQueryChange = { query = it },
                expanded = expanded,
                onExpandedChange = { expanded = it },
                filteredLanguages = filteredLanguages,
                onLanguageSelected = onLanguageSelected,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // (nur wenn keine Suche aktiv)
        if (query.isEmpty()) {

            // Zuletzt verwendet
            item {
                Text(
                    text = "Zuletzt",//stringResource(R.string.recently_used),
                    style = MaterialTheme.typography.bodySmall,
                    color = primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )
            }

            items(recentLanguages) { language ->
                LanguageCard(
                    language = language,
                    selected = false,
                    modifier = Modifier.clickable {
                        onLanguageSelected(language)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            //Alle Sprachen
            item {
                Text(
                    text = stringResource(R.string.all_languages),
                    style = MaterialTheme.typography.bodySmall,
                    color = primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )
            }

            items(allLanguages) { language ->
                LanguageCard(
                    language = language,
                    selected = false,
                    modifier = Modifier.clickable {
                        onLanguageSelected(language)
                    }
                )
            }
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    filteredLanguages: List<LanguageData>,
    onLanguageSelected: (LanguageData) -> Unit,
    modifier: Modifier = Modifier
) {
    DockedSearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = { onExpandedChange(false) },
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                placeholder = { Text(stringResource(R.string.search_language)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.delete))
                        }
                    }
                }
            )
        },
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier.padding(bottom = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn {
            items(filteredLanguages) { language ->
                LanguageCard(
                    language = language,
                    selected = false,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .clickable {
                            onLanguageSelected(language)
                            onExpandedChange(false)
                        }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}


@Composable
fun LanguageCard(
    language: LanguageData = LanguageDataSource.languagesMap.getValue(Language.GERMAN),
    selected: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card (
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(50.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) secondary else tertiary
        ),
    ) {
        Row (
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Image(
                painter = painterResource(language.flag),
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )
            Text(
                text = stringResource(language.languageName),
                modifier = Modifier.padding(start = 12.dp),
                color = if (selected) tertiary else secondary
            )
        }
    }
}


//@Preview
//@Composable
//private fun LanguageSearchBarPreview() {
//    TalkBridgeLiveTheme {
//        LanguageSearchBar()
//    }
//}

@Preview
@Composable
private fun LanguageCardPreview() {
    TalkBridgeLiveTheme {
        LanguageCard()
    }
}

@Preview(showBackground = true)
@Composable
private fun LanguageSelectScreenPreview() {
    TalkBridgeLiveTheme {
        LanguageSelectScreen(
            onBackButtonClick = {},
            languageType = "source",
            onLanguageSelected = {}
        )
    }
}




