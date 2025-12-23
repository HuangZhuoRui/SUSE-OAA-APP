package com.suseoaa.projectoaa.feature.my

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val SETTINGS_ROUTE = "settings_route"

fun NavController.navigateToMy(navOptions: NavOptions? = null) {
    this.navigate(SETTINGS_ROUTE, navOptions)
}

fun NavGraphBuilder.MyScreen(
    onBackClick: () -> Unit
) {
    composable(route = SETTINGS_ROUTE) {
        MyScreen(onBackClick = onBackClick)
    }
}