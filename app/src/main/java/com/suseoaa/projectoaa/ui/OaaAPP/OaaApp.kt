package com.suseoaa.projectoaa.ui.OaaAPP

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.suseoaa.projectoaa.feature.home.CHAT_ROUTE
import com.suseoaa.projectoaa.feature.home.COURSE_ROUTE
import com.suseoaa.projectoaa.feature.home.HOME_ROUTE
import com.suseoaa.projectoaa.feature.home.PERSON_ROUTE
import com.suseoaa.projectoaa.ui.AppNavHost.AppNavHost
import com.suseoaa.projectoaa.ui.AppState.OaaAppState
import com.suseoaa.projectoaa.ui.AppState.rememberOaaAppState

@Composable
fun OaaApp(
    windowSizeClass: WindowWidthSizeClass,
    appState: OaaAppState = rememberOaaAppState(windowSizeClass)
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // 左侧侧边栏
        if (appState.shouldShowNavRail) {
            OaaNavRail(
                currentDestination = appState.currentDestination,
                onNavigate = appState::navigateToTopLevelDestination
            )
        }

        Scaffold(
            // 底部导航栏
            bottomBar = {
                if (appState.shouldShowBottomBar) {
                    OaaBottomBar(
                        currentDestination = appState.currentDestination,
                        onNavigate = appState::navigateToTopLevelDestination
                    )
                }
            }
        ) { padding ->
            // 路由容器
            AppNavHost(
                navController = appState.navController,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

//手机端的自适应样式
@Composable
fun OaaBottomBar(
    currentDestination: NavDestination?,
    onNavigate: (String) -> Unit
) {
    NavigationBar{
        val selected = currentDestination?.hierarchy?.any { it.route == HOME_ROUTE } == true
        NavigationRailItem(
            selected = selected,
            onClick = { onNavigate(HOME_ROUTE) },
            icon = { Icon(Icons.Default.Home, contentDescription = "首页") },
            label = { Text("首页") }
        )
        NavigationRailItem(
            selected = selected,
            onClick = { onNavigate(COURSE_ROUTE) },
            icon = { Icon(Icons.Default.Book, contentDescription = "首页") },
            label = { Text("课程") }
        )
        NavigationRailItem(
            selected = selected,
            onClick = { onNavigate(CHAT_ROUTE) },
            icon = { Icon(Icons.Default.ChatBubble, contentDescription = "首页") },
            label = { Text("协会日记") }
        )
        NavigationRailItem(
            selected = selected,
            onClick = { onNavigate(PERSON_ROUTE) },
            icon = { Icon(Icons.Default.Person, contentDescription = "个人") },
            label = { Text("个人") }
        )
    }
}

//平板端的自适应样式
@Composable
fun OaaNavRail(
    currentDestination: NavDestination?,
    onNavigate: (String) -> Unit
) {
    NavigationRail {
        val selected = currentDestination?.hierarchy?.any { it.route == it.route } == true
        NavigationRailItem(
            selected = selected,
            onClick = { onNavigate(HOME_ROUTE) },
            icon = { Icon(Icons.Default.Home, contentDescription = "首页") },
            label = { Text("首页") }
        )
        NavigationRailItem(
            selected = selected,
            onClick = { onNavigate(COURSE_ROUTE) },
            icon = { Icon(Icons.Default.Book, contentDescription = "首页") },
            label = { Text("课程") }
        )
        NavigationRailItem(
            selected = selected,
            onClick = { onNavigate(CHAT_ROUTE) },
            icon = { Icon(Icons.Default.ChatBubble, contentDescription = "首页") },
            label = { Text("协会日记") }
        )
        NavigationRailItem(
            selected = selected,
            onClick = { onNavigate(PERSON_ROUTE) },
            icon = { Icon(Icons.Default.Person, contentDescription = "个人") },
            label = { Text("个人") }
        )
    }
}