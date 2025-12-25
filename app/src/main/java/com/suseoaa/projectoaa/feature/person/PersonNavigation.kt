package com.suseoaa.projectoaa.feature.person

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val PERSON_ROUTE = "person_route"

fun NavController.onNavigateToPerson(navOptions: NavOptions? = null) {
    this.navigate(PERSON_ROUTE, navOptions)
}

fun NavGraphBuilder.personScreen(
    onBackClick: () -> Unit
) {
    composable(route = PERSON_ROUTE) {
        personScreen(onBackClick = onBackClick)
    }
}