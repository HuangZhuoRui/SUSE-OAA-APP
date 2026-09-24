package com.suseoaa.projectoaa.shared.data.remote.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * OAA 客户端取 token、换 token 的入口。
 *
 * 网络层只知道「有个 token」「401 了要换一个」，token 存在哪、怎么换由数据层实现，
 * 这样 core:network 不必反向依赖 datastore。
 */
interface OaaAuthenticator {

    /** 当前 access token。构造请求头时不能挂起，所以是同步读取。 */
    fun currentAccessToken(): String?

    /**
     * 用 refresh token 换新的 access token。
     *
     * [expiredToken] 是收到 401 的那次请求所带的 token：并发请求同时 401 时，
     * 只有第一个真正去刷新，其余的发现 token 已经换过就直接用新的。
     *
     * @return 新 token；换不到（refresh token 失效、网络失败）返回 null
     */
    suspend fun refreshAccessToken(expiredToken: String?): String?
}

/**
 * 登录态失效的全局通知。
 *
 * refresh token 也失效时，数据层会清空会话并在这里发一个事件，
 * 由应用壳层（导航）统一跳回登录页，各个页面不用各自处理。
 */
class OaaSessionEvents {
    private val _expired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val expired: SharedFlow<Unit> = _expired.asSharedFlow()

    fun notifyExpired() {
        _expired.tryEmit(Unit)
    }
}
