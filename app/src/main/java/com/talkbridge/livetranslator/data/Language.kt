package com.talkbridge.livetranslator.data

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.talkbridge.livetranslator.R

enum class Language {
    GERMAN, ENGLISH, FRENCH, RUSSIAN, POLISH
}

data class LanguageData(
    @StringRes val languageName: Int,
    @DrawableRes val flag: Int
)

object LanguageDataSource{
    val languagesMap = mapOf<Language, LanguageData>(
        Language.GERMAN to LanguageData(R.string.german, R.drawable.germany_flag_circular),
        Language.ENGLISH to LanguageData(R.string.english, R.drawable.uk_flag_circular),
        Language.FRENCH to LanguageData(R.string.french, R.drawable.france_flag_circular),
        Language.RUSSIAN to LanguageData(R.string.russian, R.drawable.russia_flag_circular),
        Language.POLISH to LanguageData(R.string.polish, R.drawable.poland_flag_circular),
    )

    val languagesList = listOf<LanguageData>(
        LanguageData(R.string.german, R.drawable.germany_flag_circular),
        LanguageData(R.string.english, R.drawable.uk_flag_circular),
        LanguageData(R.string.french, R.drawable.france_flag_circular),
        LanguageData(R.string.russian, R.drawable.russia_flag_circular),
        LanguageData(R.string.polish, R.drawable.poland_flag_circular),
    )
}