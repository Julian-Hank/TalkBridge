package com.talkbridge.livetranslator.data

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.talkbridge.livetranslator.R

enum class Language {
    CHINESE,
    DUTCH,
    ENGLISH,
    FRENCH,
    GERMAN,
    ITALIAN,
    JAPANESE,
    KOREAN,
    POLISH,
    PORTUGUESE,
    RUSSIAN,
    SPANISH,
    SWEDISH,
    TURKISH,
    UKRAINIAN,
    VIETNAMESE
}

data class LanguageData(
    @StringRes val languageName: Int,
    @DrawableRes val flag: Int
)

object LanguageDataSource{
    val languagesMap = mapOf<Language, LanguageData>(
        Language.CHINESE to LanguageData(R.string.chinese, R.drawable.china_flag_circular),
        Language.DUTCH to LanguageData(R.string.dutch, R.drawable.netherlands_flag_circular),
        Language.ENGLISH to LanguageData(R.string.english, R.drawable.uk_flag_circular),
        Language.FRENCH to LanguageData(R.string.french, R.drawable.france_flag_circular),
        Language.GERMAN to LanguageData(R.string.german, R.drawable.germany_flag_circular),
        Language.ITALIAN to LanguageData(R.string.italian, R.drawable.italy_flag_circular),
        Language.JAPANESE to LanguageData(R.string.japanese, R.drawable.japan_flag_circular),
        Language.KOREAN to LanguageData(R.string.korean, R.drawable.south_korea_flag_circular),
        Language.POLISH to LanguageData(R.string.polish, R.drawable.poland_flag_circular),
        Language.PORTUGUESE to LanguageData(R.string.portuguese, R.drawable.portugal_flag_circular),
        Language.RUSSIAN to LanguageData(R.string.russian, R.drawable.russia_flag_circular),
        Language.SPANISH to LanguageData(R.string.spanish, R.drawable.spain_flag_circular),
        Language.SWEDISH to LanguageData(R.string.swedish, R.drawable.sweden_flag_circular),
        Language.TURKISH to LanguageData(R.string.turkish, R.drawable.turkey_flag_circular),
        Language.UKRAINIAN to LanguageData(R.string.ukrainian, R.drawable.ukraine_flag_circular),
        Language.VIETNAMESE to LanguageData(R.string.vietnamese, R.drawable.vietnam_flag_circular),
    )

    val languagesList = languagesMap.values.toList()

}

private val stringResToLang = mapOf(
    R.string.dutch to "nl",
    R.string.english to "en",
    R.string.french to "fr",
    R.string.german to "de",
    R.string.italian to "it",
    R.string.japanese to "ja",
    R.string.korean to "ko",
    R.string.polish to "pl",
    R.string.portuguese to "pt",
    R.string.russian to "ru",
    R.string.spanish to "es",
    R.string.swedish to "sv",
    R.string.turkish to "tr",
    R.string.ukrainian to "uk",
    R.string.vietnamese to "vi",
    R.string.chinese to "zh"
)

private val langToLanguageObject = mapOf(
    "nl" to Language.DUTCH,
    "en" to Language.ENGLISH,
    "fr" to Language.FRENCH,
    "de" to Language.GERMAN,
    "it" to Language.ITALIAN,
    "ja" to Language.JAPANESE,
    "ko" to Language.KOREAN,
    "pl" to Language.POLISH,
    "pt" to Language.PORTUGUESE,
    "ru" to Language.RUSSIAN,
    "es" to Language.SPANISH,
    "sv" to Language.SWEDISH,
    "tr" to Language.TURKISH,
    "uk" to Language.UKRAINIAN,
    "vi" to Language.VIETNAMESE,
    "zh" to Language.CHINESE
)

fun stringResToLanguagecode(@StringRes stringRes: Int): String =
    stringResToLang[stringRes] ?: "en"

fun languagecodeToLanguageObject(languagecode: String): Language =
    langToLanguageObject[languagecode] ?: Language.ENGLISH