package com.suseoaa.projectoaa.app

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.suseoaa.projectoaa.feature.chat.chatScreen
import com.suseoaa.projectoaa.feature.course.courseScreen
import com.suseoaa.projectoaa.feature.home.HOME_ROUTE
import com.suseoaa.projectoaa.feature.home.homeScreen
import com.suseoaa.projectoaa.feature.person.navigateToPerson
import com.suseoaa.projectoaa.feature.person.personScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = HOME_ROUTE,
        modifier = modifier,
//        修改动画，将原有的渐变动画关闭
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        // 1. 注册首页
        homeScreen(
            onNavigateToPerson = {
                navController.navigateToPerson()
            }
        )

        // 2. 注册个人页
        personScreen(
            onBackClick = {
                navController.popBackStack()
            }
        )

//        3.注册课程页
        courseScreen(

        )
//        4.注册协会日记页
        chatScreen()
    }
}