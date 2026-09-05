package com.suseoaa.projectoaa.scheduling

import com.suseoaa.projectoaa.domain.checkin.SchedulerConfig

expect class PlatformCheckinScheduler {
    fun schedule(config: SchedulerConfig)
    fun cancel()
}
