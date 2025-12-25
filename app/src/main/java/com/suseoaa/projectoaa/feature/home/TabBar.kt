package com.suseoaa.projectoaa.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.suseoaa.projectoaa.app.AppNavHost
import com.suseoaa.projectoaa.feature.chat.CHAT_ROUTE
import com.suseoaa.projectoaa.feature.course.COURSE_ROUTE
import com.suseoaa.projectoaa.feature.home.component.NavigationItemForPad
import com.suseoaa.projectoaa.feature.person.PERSON_ROUTE

//统一定义底部栏信息
enum class TopLevelDestination(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    HOME(HOME_ROUTE, Icons.Default.Home, "首页"),
    COURSE(COURSE_ROUTE, Icons.Default.Book, "课程信息"),
    CHAT(CHAT_ROUTE, Icons.Default.ChatBubble, "协会日记"),
    PERSON(PERSON_ROUTE, Icons.Default.Person, "个人"),
}
fun NavController.navigateToTopLevelDestination(route: String) {
    this.navigate(route) {
        // 弹出到起始页 (HOME)，避免返回栈堆积
        popUpTo(this@navigateToTopLevelDestination.graph.findStartDestination().id) {
            saveState = true
        }
        // 避免在栈顶重复创建同一页面
        launchSingleTop = true
        // 恢复之前的状态 (如滚动位置)
        restoreState = true
    }
}
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
                onNavigate = { route -> appState.navController.navigateToTopLevelDestination(route) }
            )
        }

        Scaffold(
            // 底部导航栏
            bottomBar = {
                if (appState.shouldShowBottomBar) {
                    OaaBottomBar(
                        currentDestination = appState.currentDestination,
                        onNavigate = { route -> appState.navController.navigateToTopLevelDestination(route) }
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
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        containerColor = Color.Transparent,
    ) {
        Surface(
            tonalElevation = 2.dp,
            shadowElevation = 1.dp,
            modifier = Modifier
                .padding(10.dp),
            shape = RoundedCornerShape(26.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopLevelDestination.entries.forEach { destination ->
                    // 判断当前是否选中
                    val isSelected =
                        currentDestination?.hierarchy?.any { it.route == destination.route } == true

                    NavigationRailItem(
                        selected = isSelected,
                        onClick = { onNavigate(destination.route) },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    }
}

//平板端的自适应样式
@Composable
fun OaaNavRail(
    currentDestination: NavDestination?,
    onNavigate: (String) -> Unit
) {
    NavigationRail {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.2f)
        ) {
            TopLevelDestination.entries.forEach { destination ->
                val isSelected =
                    currentDestination?.hierarchy?.any { it.route == destination.route } == true

                NavigationItemForPad(
                    selected = isSelected,
                    onClick = { onNavigate(destination.route) },
                    icon = destination.icon,
                    label = destination.label
                )
            }
        }
    }
}