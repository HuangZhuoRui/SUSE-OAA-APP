package com.suseoaa.projectoaa.home.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.suseoaa.projectoaa.home.ui.home.HomeScreenRoute

@Composable
fun MainScreen(widthSizeClass: WindowWidthSizeClass) {
    val navController = rememberNavController()
    // 判断屏幕类型
    val isCompact = widthSizeClass == WindowWidthSizeClass.Compact

    Row(modifier = Modifier.fillMaxSize()) {
        // 平板侧边栏
        if (!isCompact) {
            AppNavRail(navController)
        }

        Scaffold(
            // 手机底部栏
            bottomBar = {
                if (isCompact) {
                    AppBottomBar(navController)
                }
            }
        ) { innerPadding ->
            // 内容区域
            Box(modifier = Modifier.padding(innerPadding)) {
                // 这里我们不需要传 ViewModel，因为 HomeScreenRoute 内部自己会处理
                AppNavHost(navController)
            }
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            // 直接调用包含 MVVM 逻辑的 Route
            HomeScreenRoute()
        }
        composable("settings") {
            // 这里可以放设置页
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("设置页") }
        }
    }
}

// --- 下面是导航组件，保持不变 ---
@Composable
fun AppBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick = { navController.navigate("home") },
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("首页") }
        )
        NavigationBarItem(
            selected = currentRoute == "settings",
            onClick = { navController.navigate("settings") },
            icon = { Icon(Icons.Default.Settings, null) },
            label = { Text("设置") }
        )
    }
}

@Composable
fun AppNavRail(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    NavigationRail {
        NavigationRailItem(
            selected = currentRoute == "home",
            onClick = { navController.navigate("home") },
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("首页") }
        )
        NavigationRailItem(
            selected = currentRoute == "settings",
            onClick = { navController.navigate("settings") },
            icon = { Icon(Icons.Default.Settings, null) },
            label = { Text("设置") }
        )
    }
}