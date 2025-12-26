package com.suseoaa.projectoaa.feature.textScreen

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val test_ROUTE = "test_route"

fun NavController.navigateToPerson(navOptions: NavOptions? = null) {
    this.navigate(test_ROUTE, navOptions)
}

fun NavGraphBuilder.testScreen(
) {
    composable(route = test_ROUTE) {
        TestScreen()
    }
}