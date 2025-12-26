package com.suseoaa.projectoaa.feature.home

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.suseoaa.projectoaa.feature.chat.CHAT_ROUTE
import com.suseoaa.projectoaa.feature.chat.navigateToChat
import com.suseoaa.projectoaa.feature.course.COURSE_ROUTE
import com.suseoaa.projectoaa.feature.course.navigateToCourse
import com.suseoaa.projectoaa.feature.login.LOGIN_ROUTE
import com.suseoaa.projectoaa.feature.person.PERSON_ROUTE
import com.suseoaa.projectoaa.feature.person.navigateToPerson

@Stable
class OaaAppState(
    val navController: NavHostController,
    val windowSizeClass: WindowWidthSizeClass
) {
    // 获取当前路由，用于高亮导航栏图标
    val currentDestination: NavDestination?
        @Composable get() = navController
            .currentBackStackEntryAsState().value?.destination

    // 判断是否显示底部栏，手机
    val shouldShowBottomBar: Boolean
        @Composable
        get() = windowSizeClass == WindowWidthSizeClass.Compact
                // 如果是登录页，这就返回 false
                && currentDestination?.route != LOGIN_ROUTE

    // 判断是否显示侧边栏,平板
    val shouldShowNavRail: Boolean
        @Composable
        get() = windowSizeClass != WindowWidthSizeClass.Compact
                // 如果是登录页，这就返回 false
                && currentDestination?.route != LOGIN_ROUTE
}

// 帮助函数：方便在 UI 中创建 AppState
@Composable
fun rememberOaaAppState(
    windowSizeClass: WindowWidthSizeClass,
    navController: NavHostController = rememberNavController()
): OaaAppState {
    return remember(navController, windowSizeClass) {
        OaaAppState(navController, windowSizeClass)
    }
}