package com.suseoaa.projectoaa.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.suseoaa.projectoaa.feature.home.HOME_ROUTE
import com.suseoaa.projectoaa.feature.home.homeScreen
import com.suseoaa.projectoaa.feature.person.onNavigateToPerson
import com.suseoaa.projectoaa.feature.person.personScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = HOME_ROUTE,
        modifier = modifier
    ) {
        // 1. 注册首页
        homeScreen(
            onNavigateToPerson = {
                navController.onNavigateToPerson()
            }
        )

        // 2. 注册个人页
        personScreen(
            onBackClick = {
                navController.popBackStack()
            }
        )

//        3.注册课程页
    }
}