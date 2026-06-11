package com.talkbridge.livetranslator.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml


@Composable
fun htmlStringResource(id: Int): AnnotatedString {
    val context = LocalContext.current
    // Wir holen den String als String (inklusive eventueller Maskierungen wie &lt;i&gt;)
    val rawString = LocalResources.current.getString(id)

    // AnnotatedString.fromHtml konvertiert <i>, <b> etc. direkt in Compose-Styles
    return AnnotatedString.fromHtml(rawString)
}