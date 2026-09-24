package com.suseoaa.projectoaa.shared.data.remote

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.suseoaa.projectoaa.shared.data.local.store.SessionStore
import com.suseoaa.projectoaa.shared.data.remote.api.OaaRequester
import com.suseoaa.projectoaa.shared.data.remote.network.OaaHttpClient
import com.suseoaa.projectoaa.shared.data.remote.network.OaaSessionEvents
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json

internal val testJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

/** 内存版 DataStore，行为与真实实现一致：每次 edit 产生一份新的 Preferences。 */
internal class InMemoryPreferences : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())
    override val data: StateFlow<Preferences> = state
    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        transform(state.value).also { state.value = it }
}

internal fun MockRequestHandleScope.json(
    body: String,
    status: HttpStatusCode = HttpStatusCode.OK
): HttpResponseData = respond(
    content = body,
    status = status,
    headers = headersOf(HttpHeaders.ContentType, "application/json")
)

internal fun HttpRequestData.bodyText(): String =
    (body as? OutgoingContent.ByteArrayContent)?.bytes()?.decodeToString().orEmpty()

internal fun HttpRequestData.bearer(): String? =
    headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")

/** 按真实装配方式组好的 OAA 网络栈，服务端行为由 [handler] 模拟。 */
internal class OaaTestStack(
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
) {
    val requests = mutableListOf<HttpRequestData>()
    val sessionStore = SessionStore(InMemoryPreferences())
    val events = OaaSessionEvents()
    var sessionCleared = false

    private val engine = MockEngine { request ->
        requests += request
        handler(request)
    }

    val tokenManager = OaaTokenManager(
        sessionStore = sessionStore,
        bareRequester = OaaRequester(OaaHttpClient.createBare(testJson, engine), testJson),
        sessionEvents = events,
        clearSession = {
            sessionCleared = true
            sessionStore.clear()
        }
    )

    val requester = OaaRequester(OaaHttpClient.create(testJson, tokenManager, engine), testJson)

    fun requestsTo(path: String) = requests.filter { it.url.encodedPath == "/v2$path" }
}
