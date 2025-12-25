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

@Stable
class OaaAppState(
    val navController: NavHostController,
    val windowSizeClass: WindowWidthSizeClass
) {
    // 获取当前路由，用于高亮导航栏图标
    val currentDestination: NavDestination?
        @Composable get() = navController
            .currentBackStackEntryAsState().value?.destination

    // 判断是否显示底部栏 (手机竖屏)
    val shouldShowBottomBar: Boolean
        get() = windowSizeClass == WindowWidthSizeClass.Compact

    // 判断是否显示侧边栏 (平板/横屏)
    val shouldShowNavRail: Boolean
        get() = windowSizeClass != WindowWidthSizeClass.Compact

    // 封装顶级页面的跳转逻辑 (避免多层堆栈)
    fun navigateToTopLevelDestination(destinationRoute: String) {
        val topLevelNavOptions = navOptions {
            // 弹出到起始页
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }

        when (destinationRoute) {
            HOME_ROUTE -> navController.navigateToHome(topLevelNavOptions)
            // 如果以后有其他 Tab，在这里添加
        }
    }
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