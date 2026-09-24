package com.suseoaa.projectoaa.shared.data.remote

import com.suseoaa.projectoaa.shared.data.local.store.SessionStore
import com.suseoaa.projectoaa.shared.data.remote.api.OaaRequester
import com.suseoaa.projectoaa.shared.data.remote.network.OaaAuthenticator
import com.suseoaa.projectoaa.shared.data.remote.network.OaaSessionEvents
import com.suseoaa.projectoaa.shared.domain.error.AppError
import com.suseoaa.projectoaa.shared.domain.error.AppException
import com.suseoaa.projectoaa.shared.domain.model.login.RefreshTokenRequest
import com.suseoaa.projectoaa.shared.domain.model.login.TokenPair
import com.suseoaa.projectoaa.shared.util.AppLog
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 用 refresh token 续期 access token。
 *
 * - 并发的多个请求同时 401 时只刷新一次，其余的直接拿刷新后的 token 重放；
 * - 只有后端明确拒绝（refresh token 失效）才清空会话并通知跳转登录页，
 *   断网、超时之类的失败不会把用户踢下线。
 *
 * [bareRequester] 必须基于不带 401 拦截的客户端，否则刷新请求自己 401 会递归刷新。
 */
class OaaTokenManager(
    private val sessionStore: SessionStore,
    private val bareRequester: OaaRequester,
    private val sessionEvents: OaaSessionEvents,
    private val clearSession: suspend () -> Unit
) : OaaAuthenticator {

    private val mutex = Mutex()

    override fun currentAccessToken(): String? = sessionStore.cachedToken

    override suspend fun refreshAccessToken(expiredToken: String?): String? = mutex.withLock {
        val current = sessionStore.cachedToken
        if (!current.isNullOrEmpty() && current != expiredToken) return@withLock current

        val refreshToken = sessionStore.getRefreshToken()
        val userId = sessionStore.getUserId()
        if (refreshToken.isNullOrEmpty() || userId == null) {
            expire()
            return@withLock null
        }

        val result = bareRequester.execute<TokenPair>(
            fallbackMessage = "刷新登录状态失败",
            decode = { bareRequester.decode<TokenPair>(it) }
        ) {
            bareRequester.client.post(ApiConfig.OAA_BASE + "/auth/refresh") {
                contentType(ContentType.Application.Json)
                // 文档要求该接口带 bearer；带上过期的 token，后端不需要时会忽略
                if (!expiredToken.isNullOrEmpty()) header(HttpHeaders.Authorization, "Bearer $expiredToken")
                setBody(RefreshTokenRequest(refreshToken = refreshToken, userId = userId, device = oaaDevice))
            }
        }

        result.fold(
            onSuccess = { tokens ->
                if (tokens.token.isEmpty()) {
                    expire()
                    null
                } else {
                    // 后端可能轮换 refresh token，没返回新的就沿用旧的
                    sessionStore.saveTokens(tokens.token, tokens.refreshToken.ifEmpty { refreshToken })
                    tokens.token
                }
            },
            onFailure = { error ->
                val appError = (error as? AppException)?.error
                if (appError is AppError.Network) {
                    AppLog.w("刷新 token 时网络异常，保留当前会话", TAG, error)
                } else {
                    AppLog.w("refresh token 已失效，清空会话", TAG, error)
                    expire()
                }
                null
            }
        )
    }

    private suspend fun expire() {
        clearSession()
        sessionEvents.notifyExpired()
    }

    private companion object {
        const val TAG = "OaaTokenManager"
    }
}
