package com.suseoaa.projectoaa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.suseoaa.projectoaa.feature.home.OaaApp
import com.suseoaa.projectoaa.core.designsystem.theme.ProjectOAATheme
import dagger.hilt.android.AndroidEntryPoint

// 定义全局 CompositionLocal，用于在任何地方获取屏幕尺寸类别
val LocalWindowSizeClass = staticCompositionLocalOf<WindowSizeClass> {
    error("No WindowSizeClass provided")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // 1. 计算当前的 WindowSizeClass
            val windowSizeClass = calculateWindowSizeClass(this)

            // 2. 使用 CompositionLocalProvider 将其向下传递
            CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
                ProjectOAATheme {
                    // 3. 正常启动 App
                    OaaApp(windowSizeClass = windowSizeClass.widthSizeClass)
                }
            }
        }
    }
}