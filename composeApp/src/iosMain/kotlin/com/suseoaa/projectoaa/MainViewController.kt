package com.suseoaa.projectoaa

import androidx.compose.ui.window.ComposeUIViewController
import com.suseoaa.projectoaa.presentation.checkin.CheckinScheduler
import com.suseoaa.projectoaa.scheduling.initializeKoinIfNeeded
import com.suseoaa.projectoaa.util.AppLifecycleObserver
import org.koin.mp.KoinPlatform

fun MainViewController() = ComposeUIViewController(
    configure = {
        initializeKoinIfNeeded()

        // 启动定时签到调度器
        try {
            val koin = KoinPlatform.getKoin()
            val scheduler = koin.get<CheckinScheduler>()
            scheduler.start()

            val observer = AppLifecycleObserver()
            observer.startObserving(
                onForeground = { scheduler.onAppForeground() },
                onBackground = { scheduler.onAppBackground() }
            )
        } catch (e: Exception) {
            println("[MainViewController] 启动定时签到调度器失败: ${e.message}")
        }
    }
) {
    App()
}
