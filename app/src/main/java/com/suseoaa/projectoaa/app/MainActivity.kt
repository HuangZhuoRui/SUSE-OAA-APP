package com.suseoaa.projectoaa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.suseoaa.projectoaa.core.ui.oaaAPP.tabBar.OaaApp
import com.suseoaa.projectoaa.core.designsystem.theme.ProjectOAATheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProjectOAATheme {
                val windowSizeClass = calculateWindowSizeClass(this)
                OaaApp(windowSizeClass = windowSizeClass.widthSizeClass)
            }
        }
    }
}