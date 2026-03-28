package com.talkbridge.livetranslator.ui.navigation


interface NavigationDestination {
    val route: String

    val titleRes: Int

}

interface NavigationDestinationWithIcon : NavigationDestination {
    val icon: Int
}
