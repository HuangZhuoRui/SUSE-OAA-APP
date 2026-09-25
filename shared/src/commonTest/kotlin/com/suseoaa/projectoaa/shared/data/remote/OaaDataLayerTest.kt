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

    /** 以下响应取自线上后端（2026-09 抓取、已脱敏），文档里这几个接口没写结构。 */
    @Test
    fun recruitmentReadsMatchRealBackendShapes() = runTest {
        val stack = OaaTestStack { request ->
            when (request.url.encodedPath) {
                "/v2/term/list" -> json(
                    """{"code":200,"message":"success","data":[{"id":3,"year":2026,"type":"招新","title":"秋招",
                    "edit_period":{"start_at":"2026-09-11","end_at":"2026-09-12"},
                    "query_period":{"start_at":"2026-09-13","end_at":"2026-09-30"},
                    "is_executed":false,"execute_after_at":"2026-10-01T00:00:59+08:00","executed_at":null,
                    "created_at":"2026-09-11T15:35:31.892+08:00","updated_at":"2026-09-14T14:35:20.81+08:00"}]}"""
                )
                "/v2/application/me" -> json("""{"code":200,"message":"success","data":[]}""")
                "/v2/application/list" -> json(
                    """{"code":200,"message":"success","data":[{"id":6,"term_id":3,"term_title":"秋招","type":"招新",
                    "user_id":80,"name":"张三","gender":"男","avatar":{"url":"","uri":""},"student_id":"20240001",
                    "college":"计科院","major_class":"计科241","political_status":"共青团员","birth_date":"2005-09-01",
                    "qq":"12345","phone":"13800000000","first_choice":{"department_id":1,"role_id":6},
                    "second_choice":{"department_id":3,"role_id":6},"allow_adjust":true,"resume":"r","reason":"r",
                    "decision":"待定","result":{"department_id":0,"role_id":0},"operator_user_id":0,
                    "decision_remark":"","created_at":"2026-09-13T18:41:56.511+08:00","updated_at":"2026-09-13T18:41:56.511+08:00"}]}"""
                )
                "/v2/interviewer/result/list" -> json(
                    """{"code":200,"message":"success","data":[{"id":1,"term_id":3,"application_id":6,"type":"招新",
                    "user_id":80,"decision":"录取第一志愿","result_department_id":1,"result_role_id":6,
                    "old":{"department_id":0,"role_id":0},"executed_at":null,"operator_user_id":80,"remark":"",
                    "created_at":"2026-09-17T16:08:25.536+08:00","updated_at":"2026-09-17T17:04:29.876+08:00",
                    "name":"张三","operator_name":"李四"}]}"""
                )
                "/v2/application/role" -> json(
                    """{"code":200,"message":"success","data":[{"id":6,"name":"干事","level":20,"type":"部门",
                    "is_active":true,"created_at":"2026-09-04T14:36:25.422+08:00","updated_at":"2026-09-04T14:36:25.422+08:00"}]}"""
                )
                "/v2/interviewer/result/decision" -> json(
                    """{"code":200,"message":"success","data":["录取第一志愿","录取第二志愿","未通过"]}"""
                )
                else -> error("unexpected ${request.url}")
            }
        }
        val repo = RecruitmentRepositoryImpl(OaaRecruitmentApi(stack.requester), testJson)

        val term = repo.getTerms().getOrThrow().single()
        assertEquals(3, term.resolvedId, "列表只返回 id，没有 term_id")
        assertNull(term.executedAt)

        assertTrue(repo.getMyApplications().getOrThrow().isEmpty())

        val application = repo.getApplications(termId = 3, departmentId = null).getOrThrow().single()
        assertEquals(6, application.resolvedId)
        assertEquals("张三", application.name)
        assertEquals(Choice(3, 6), application.secondChoice)
        assertEquals("3", stack.requestsTo("/application/list").single().url.parameters["term_id"])

        val result = repo.getInterviewResults(termId = 3).getOrThrow().single()
        assertEquals(1, result.resolvedId)
        assertEquals(6, result.applicationId)
        assertEquals(1, result.resultDepartmentId)

        // 可申请职位的 id 必须能解析出来，否则申请表的志愿永远选不完整
        assertEquals(listOf(6), repo.getFillableRoles(departmentId = 1).getOrThrow().map { it.id })
        assertEquals(listOf("录取第一志愿", "录取第二志愿", "未通过"), repo.getDecisions().getOrThrow())
    }

    @Test
    fun personReadsRoleLevelFromBackend() {
        val person = testJson.decodeFromString<PersonData>("""{"user_id":33,"role":"开发者","role_level":100}""")
        assertEquals(100, person.roleLevel)
        assertNull(testJson.decodeFromString<PersonData>("""{"user_id":33}""").roleLevel)
    }

    @Test
    fun nonJsonServerErrorBecomesHttpError() = runTest {
        val stack = OaaTestStack { respondError(HttpStatusCode.BadGateway, "<html>502 Bad Gateway</html>") }

        val error = (OaaUserApi(stack.requester).me().exceptionOrNull() as AppException).error

        assertIs<AppError.Http>(error)
        assertEquals(502, error.status)
    }
}
