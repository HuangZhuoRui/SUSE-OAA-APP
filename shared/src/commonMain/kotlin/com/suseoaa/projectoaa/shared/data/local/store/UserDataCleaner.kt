package com.suseoaa.projectoaa.shared.data.local.store

import com.suseoaa.projectoaa.shared.data.remote.network.SessionCleaner

/**
 * 退出登录时的统一清理入口。
 *
 * 会话数据分散在三个 store 里，清理必须一起做；把顺序集中到这里，
 * 免得各调用方各清一半（旧代码里 clearSession 塞在 TokenManager 内部，
 * 导致"哪些键属于会话"这件事和"谁负责主题设置"混在同一个类里）。
 * 主题、学期、应用设置等不随登录态清除，因此不在此列。
 */
class UserDataCleaner(
    private val sessionStore: SessionStore,
    private val credentialStore: CredentialStore,
    private val userProfileStore: UserProfileStore,
) {
    suspend fun clearSession() {
        sessionStore.clear()
        credentialStore.clear()
        userProfileStore.clear()
        // 清掉各 HttpClient 的 Cookie，否则下一个账号会复用上一个人的会话
        SessionCleaner.clearAllNetworkSessions()
    }
}
