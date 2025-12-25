package com.suseoaa.projectoaa.feature.chat

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val CHAT_ROUTE = "chat_route"

fun NavController.navigateToChat(navOptions: NavOptions? = null) {
    this.navigate(CHAT_ROUTE, navOptions)
}

fun NavGraphBuilder.chatScreen() {
    composable(route = CHAT_ROUTE) {
        ChatScreen()
    }
}