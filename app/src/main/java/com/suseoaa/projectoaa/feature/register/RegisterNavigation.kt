package com.suseoaa.projectoaa.feature.register

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val REGISTER_ROUTE = "register_route"

fun NavController.navigateToRegister(navOptions: NavOptions? = null) {
    this.navigate(REGISTER_ROUTE, navOptions)
}

fun NavGraphBuilder.registerScreen(
) {
    composable(route = REGISTER_ROUTE) {
        RegisterScreen()
    }
}