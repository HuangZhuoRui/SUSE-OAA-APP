package com.suseoaa.projectoaa.shared.data.remote

import com.suseoaa.projectoaa.shared.data.remote.api.OaaAnnouncementApi
import com.suseoaa.projectoaa.shared.data.remote.api.OaaAuthApi
import com.suseoaa.projectoaa.shared.data.remote.api.OaaRecruitmentApi
import com.suseoaa.projectoaa.shared.data.remote.api.OaaUserApi
import com.suseoaa.projectoaa.shared.data.repository.AnnouncementRepositoryImpl
import com.suseoaa.projectoaa.shared.data.repository.OaaAuthRepositoryImpl
import com.suseoaa.projectoaa.shared.data.repository.RecruitmentRepositoryImpl
import com.suseoaa.projectoaa.shared.domain.error.AppError
import com.suseoaa.projectoaa.shared.domain.error.AppException
import com.suseoaa.projectoaa.shared.domain.model.person.PersonData
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Application
import com.suseoaa.projectoaa.shared.domain.model.recruitment.ApplicationForm
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Choice
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OaaDataLayerTest {

    private val meBody = """
        {"code":200,"message":"success","data":{"user_id":7,"student_id":"20240001","username":"zhangsan",
        "avatar":{"uri":"img/avatar/a.jpg","url":"signed-url-a"},"name":"张三","email":"z@x.com",
        "department":"算法竞赛部","role":"部长"}}
    """.trimIndent()

    @Test
    fun loginSavesTokensUserIdAndRealStudentId() = runTest {
        val stack = OaaTestStack { request ->
            when (request.url.encodedPath) {
                "/v2/auth/login" -> json("""{"code":200,"message":"success","data":{"token":"t1","refresh_token":"r1"}}""")
                "/v2/user/me" -> json(meBody)
                else -> error("unexpected ${request.url}")
            }
        }
        val repo = OaaAuthRepositoryImpl(OaaAuthApi(stack.requester), OaaUserApi(stack.requester), stack.sessionStore) {}

        val person = repo.login("zhangsan", "pwd").getOrThrow()

        assertEquals("20240001", person.studentId)
        val body = testJson.parseToJsonElement(stack.requestsTo("/auth/login").single().bodyText()).jsonObject
        assertEquals("zhangsan", body["account"]?.jsonPrimitive?.content)
        assertEquals("android", body["device"]?.jsonPrimitive?.content)
        assertEquals("t1", stack.sessionStore.cachedToken)
        assertEquals("r1", stack.sessionStore.getRefreshToken())
        assertEquals(7, stack.sessionStore.getUserId())
        // 用用户名登录，存下来的必须是真实学号，教务功能靠它匹配账号
        assertEquals("20240001", stack.sessionStore.currentStudentId.first())
        assertEquals("Bearer t1", stack.requestsTo("/user/me").single().headers["Authorization"])
    }

    @Test
    fun loginFailureSurfacesBackendMessage() = runTest {
        val stack = OaaTestStack {
            json("""{"code":400,"message":"用户名或密码错误","data":null}""", HttpStatusCode.BadRequest)
        }
        val repo = OaaAuthRepositoryImpl(OaaAuthApi(stack.requester), OaaUserApi(stack.requester), stack.sessionStore) {}

        val error = repo.login("x", "y").exceptionOrNull()

        assertEquals("用户名或密码错误", error?.message)
        assertNull(stack.sessionStore.cachedToken)
    }

    @Test
    fun expiredTokenIsRefreshedOnceAndRequestReplayed() = runTest {
        val stack = OaaTestStack { request ->
            when (request.url.encodedPath) {
                "/v2/auth/refresh" -> json("""{"code":200,"message":"success","data":{"token":"new","refresh_token":"r2"}}""")
                "/v2/user/me" ->
                    if (request.bearer() == "new") json(meBody)
                    else json("""{"code":401,"message":"token 已过期","data":null}""", HttpStatusCode.Unauthorized)
                else -> error("unexpected ${request.url}")
            }
        }
        stack.sessionStore.saveTokens("old", "r1")
        stack.sessionStore.saveUserId(7)
        val api = OaaUserApi(stack.requester)

        val results = listOf(async { api.me() }, async { api.me() }).awaitAll()

        results.forEach { assertEquals("zhangsan", it.getOrThrow().username) }
        val refresh = stack.requestsTo("/auth/refresh")
        assertEquals(1, refresh.size, "并发 401 只应刷新一次")
        val body = testJson.parseToJsonElement(refresh.single().bodyText()).jsonObject
        assertEquals("r1", body["refresh_token"]?.jsonPrimitive?.content)
        assertEquals("7", body["user_id"]?.jsonPrimitive?.content)
        assertEquals("new", stack.sessionStore.cachedToken)
        assertEquals("r2", stack.sessionStore.getRefreshToken())
        assertFalse(stack.sessionCleared)
    }

    @Test
    fun rejectedRefreshClearsSessionAndNotifies() = runTest {
        val stack = OaaTestStack { request ->
            when (request.url.encodedPath) {
                "/v2/auth/refresh" -> json("""{"code":400,"message":"token 无效","data":null}""", HttpStatusCode.BadRequest)
                else -> json("""{"code":401,"message":"","data":null}""", HttpStatusCode.Unauthorized)
            }
        }
        stack.sessionStore.saveTokens("old", "r1")
        stack.sessionStore.saveUserId(7)
        val expired = async(start = CoroutineStart.UNDISPATCHED) { stack.events.expired.first() }

        val result = OaaUserApi(stack.requester).me()

        expired.await()
        assertTrue(stack.sessionCleared)
        assertIs<AppError.SessionExpired>((result.exceptionOrNull() as AppException).error)
    }

    @Test
    fun uploadAcceptsStringCode() = runTest {
        val stack = OaaTestStack {
            json("""{"code":"200","message":null,"data":{"uri":"bucket/a.jpg","url":"signed-url-a"}}""")
        }

        val file = OaaUserApi(stack.requester).uploadImage("avatar", byteArrayOf(1, 2), "a.jpg", "image/jpeg").getOrThrow()

        assertEquals("bucket/a.jpg", file.uri)
        val contentType = stack.requests.single().body.contentType.toString()
        assertTrue(contentType.startsWith("multipart/form-data"), contentType)
    }

    @Test
    fun personAvatarNullFallsBackToEmpty() {
        val person = testJson.decodeFromString<PersonData>("""{"user_id":1,"avatar":null,"department":null}""")
        assertEquals("", person.avatarUrl)
        assertEquals("", person.department)
    }

    @Test
    fun myApplicationsAcceptsBareObjectAndWrappedList() = runTest {
        var wrapped = false
        val stack = OaaTestStack {
            if (wrapped) json("""{"code":200,"message":"success","data":[{"term_id":1,"application_id":3},{"term_id":2}]}""")
            else json("""{"user_id":1,"term_id":1,"college":"计科院","allow_adjust":false,"first_choice":{"department_id":1,"role_id":6}}""")
        }
        val repo = RecruitmentRepositoryImpl(OaaRecruitmentApi(stack.requester), testJson)

        val bare = repo.getMyApplications().getOrThrow()
        wrapped = true
        val list = repo.getMyApplications().getOrThrow()

        assertEquals(1, bare.size)
        assertEquals(Choice(1, 6), bare.single().firstChoice)
        assertEquals(listOf(3, 0), list.map { it.resolvedId })
    }

    @Test
    fun applicationRequestsKeepFalseBooleansAndOmitUnknownIds() = runTest {
        val stack = OaaTestStack { json("""{"code":200,"message":"success","data":null}""") }
        val repo = RecruitmentRepositoryImpl(OaaRecruitmentApi(stack.requester), testJson)
        val form = ApplicationForm(allowAdjust = false, firstChoice = Choice(1, 6), secondChoice = Choice(2, 6))

        repo.createApplication(termId = 1, form = form).getOrThrow()
        repo.updateApplication(termId = 1, existing = Application(), form = form).getOrThrow()

        val create = testJson.parseToJsonElement(stack.requestsTo("/application/create").single().bodyText()).jsonObject
        assertEquals("false", create["allow_adjust"]?.jsonPrimitive?.content)
        assertEquals("1", create["term_id"]?.jsonPrimitive?.content)
        val update = testJson.parseToJsonElement(stack.requestsTo("/application/update").single().bodyText()).jsonObject
        assertFalse("application_id" in update, "拿不到 ID 时不应发送 application_id")
        assertEquals("1", update["term_id"]?.jsonPrimitive?.content)
    }

    @Test
    fun decisionsParseFromStringsOrObjects() = runTest {
        var objects = false
        val stack = OaaTestStack {
            if (objects) json("""{"code":200,"message":"success","data":[{"name":"待定"},{"value":"已录取"}]}""")
            else json("""{"code":200,"message":"success","data":["待定","已调剂"]}""")
        }
        val repo = RecruitmentRepositoryImpl(OaaRecruitmentApi(stack.requester), testJson)

        assertEquals(listOf("待定", "已调剂"), repo.getDecisions().getOrThrow())
        objects = true
        assertEquals(listOf("待定", "已录取"), repo.getDecisions().getOrThrow())
    }

    @Test
    fun createAndPublishPushesTheNewDraft() = runTest {
        val stack = OaaTestStack { request ->
            when (request.url.encodedPath) {
                "/v2/announcement/list" -> json(
                    """{"code":200,"message":"success","data":[
                    {"announcement_id":3,"title":"例会","department_name":"算法竞赛部"},
                    {"announcement_id":9,"title":"例会","department_name":"算法竞赛部"},
                    {"announcement_id":12,"title":"例会","department_name":"秘书处"}]}"""
                )
                else -> json("""{"code":200,"message":"success","data":null}""")
            }
        }
        val repo = AnnouncementRepositoryImpl(OaaAnnouncementApi(stack.requester), testJson)

        repo.createAndPublish(departmentId = 1, departmentName = "算法竞赛部", title = "例会", content = "周三").getOrThrow()

        assertEquals("draft", stack.requestsTo("/announcement/list").single().url.parameters["status"])
        val push = testJson.parseToJsonElement(stack.requestsTo("/announcement/push").single().bodyText()).jsonObject
        assertEquals("9", push["announcement_id"]?.jsonPrimitive?.content)
    }

    @Test
    fun nonJsonServerErrorBecomesHttpError() = runTest {
        val stack = OaaTestStack { respondError(HttpStatusCode.BadGateway, "<html>502 Bad Gateway</html>") }

        val error = (OaaUserApi(stack.requester).me().exceptionOrNull() as AppException).error

        assertIs<AppError.Http>(error)
        assertEquals(502, error.status)
    }
}
