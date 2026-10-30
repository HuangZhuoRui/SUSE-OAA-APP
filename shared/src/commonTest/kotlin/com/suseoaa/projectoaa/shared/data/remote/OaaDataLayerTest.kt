package com.suseoaa.projectoaa.shared.data.remote

import com.suseoaa.projectoaa.shared.data.local.store.CredentialStore
import com.suseoaa.projectoaa.shared.data.local.store.UserDataCleaner
import com.suseoaa.projectoaa.shared.data.local.store.UserProfileStore
import com.suseoaa.projectoaa.shared.data.remote.api.OaaAnnouncementApi
import com.suseoaa.projectoaa.shared.data.remote.api.OaaAuthApi
import com.suseoaa.projectoaa.shared.data.remote.api.OaaRecruitmentApi
import com.suseoaa.projectoaa.shared.data.remote.api.OaaUserApi
import com.suseoaa.projectoaa.shared.data.repository.AnnouncementRepositoryImpl
import com.suseoaa.projectoaa.shared.data.repository.OaaAuthRepositoryImpl
import com.suseoaa.projectoaa.shared.data.repository.OrganizationRepositoryImpl
import com.suseoaa.projectoaa.shared.data.repository.PersonRepositoryImpl
import com.suseoaa.projectoaa.shared.data.repository.UserManagementRepositoryImpl
import com.suseoaa.projectoaa.shared.data.remote.api.OaaOrganizationApi
import com.suseoaa.projectoaa.shared.data.repository.RecruitmentRepositoryImpl
import com.suseoaa.projectoaa.shared.domain.error.AppError
import com.suseoaa.projectoaa.shared.domain.error.AppException
import com.suseoaa.projectoaa.shared.domain.model.person.PersonData
import com.suseoaa.projectoaa.shared.domain.model.person.StoredFile
import com.suseoaa.projectoaa.shared.domain.model.person.UserBatchItem
import com.suseoaa.projectoaa.shared.domain.model.person.UserQuery
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Application
import com.suseoaa.projectoaa.shared.domain.model.recruitment.ApplicationForm
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Choice
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
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
        val form = ApplicationForm(
            avatarUri = "application/photo.jpg",
            allowAdjust = false,
            firstChoice = Choice(1, 6),
            secondChoice = Choice(2, 6)
        )

        repo.createApplication(termId = 1, form = form).getOrThrow()
        repo.updateApplication(
            termId = 1,
            existing = Application(avatar = StoredFile(uri = "application/fallback.jpg", url = "https://img/f.jpg")),
            form = form.copy(avatarUri = "")
        ).getOrThrow()

        val create = testJson.parseToJsonElement(stack.requestsTo("/application/create").single().bodyText()).jsonObject
        assertEquals("false", create["allow_adjust"]?.jsonPrimitive?.content)
        assertEquals("1", create["term_id"]?.jsonPrimitive?.content)
        assertEquals("application/photo.jpg", create["avatar"]?.jsonPrimitive?.content)
        val update = testJson.parseToJsonElement(stack.requestsTo("/application/update").single().bodyText()).jsonObject
        assertFalse("application_id" in update, "拿不到 ID 时不应发送 application_id")
        assertEquals("1", update["term_id"]?.jsonPrimitive?.content)
        assertEquals("application/fallback.jpg", update["avatar"]?.jsonPrimitive?.content)
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
    fun personParsesDeletingStatusWithAndWithoutScheduledTime() {
        val deleting = testJson.decodeFromString<PersonData>(
            """{
                "user_id": 42,
                "student_id": "24101010330",
                "username": "zhong_yan",
                "name": "钟研",
                "email": "user@example.com",
                "department": "技术部",
                "role": "部长",
                "role_level": 80,
                "avatar": { "uri": "avatar/202610.png", "url": "http://127.0.0.1:9000/a.png" },
                "is_deleting": true,
                "scheduled_delete_at": "2026-10-26 15:04:05"
            }"""
        )
        assertTrue(deleting.isDeleting)
        assertEquals("2026-10-26 15:04:05", deleting.scheduledDeleteAt)

        val normal = testJson.decodeFromString<PersonData>(
            """{
                "user_id": 42,
                "student_id": "24101010330",
                "username": "zhong_yan",
                "name": "钟研",
                "email": "user@example.com",
                "department": "技术部",
                "role": "部长",
                "role_level": 80,
                "avatar": { "uri": "avatar/202610.png", "url": "http://127.0.0.1:9000/a.png" },
                "is_deleting": false
            }"""
        )
        assertFalse(normal.isDeleting)
        assertNull(normal.scheduledDeleteAt)
    }

    private fun personRepository(stack: OaaTestStack) = PersonRepositoryImpl(
        userApi = OaaUserApi(stack.requester),
        authApi = OaaAuthApi(stack.requester),
        organizationRepository = OrganizationRepositoryImpl(OaaOrganizationApi(stack.requester)),
        userDataCleaner = UserDataCleaner(
            sessionStore = stack.sessionStore,
            credentialStore = CredentialStore(InMemoryPreferences()),
            userProfileStore = UserProfileStore(InMemoryPreferences()),
            clearNetworkSessions = {}
        )
    )

    @Test
    fun deleteAccountSendsCodeToEmailThenDeletesWithCodeOnly() = runTest {
        val stack = OaaTestStack { request ->
            when (request.url.encodedPath) {
                "/v2/user/me" -> json(meBody)
                else -> json("""{"code":200,"message":"success","data":null}""")
            }
        }
        stack.sessionStore.saveTokens("t1", "r1")
        stack.sessionStore.saveUserId(7)
        val repo = personRepository(stack)

        repo.sendDeleteAccountCode().getOrThrow()
        repo.deleteAccount("123456").getOrThrow()

        val send = testJson.parseToJsonElement(stack.requestsTo("/auth/send").single().bodyText()).jsonObject
        assertEquals("delete_user", send["scene"]?.jsonPrimitive?.content)
        assertEquals("zhangsan", send["account"]?.jsonPrimitive?.content)
        val delete = stack.requestsTo("/user/delete").single()
        assertEquals("Bearer t1", delete.headers["Authorization"])
        val body = testJson.parseToJsonElement(delete.bodyText()).jsonObject
        assertEquals("123456", body["code"]?.jsonPrimitive?.content)
        // 文档：code 和 user_id 只能出现一个，带了 user_id 会被当成管理员删人
        assertFalse("user_id" in body)
        assertEquals(1, stack.requestsTo("/auth/logout").size, "注销后应注销本设备会话")
        assertNull(stack.sessionStore.cachedToken)
        assertNull(stack.sessionStore.getRefreshToken())
    }

    @Test
    fun deleteAccountWithWrongCodeKeepsSession() = runTest {
        val stack = OaaTestStack {
            json("""{"code":400,"message":"验证码错误或已失效","data":null}""", HttpStatusCode.BadRequest)
        }
        stack.sessionStore.saveTokens("t1", "r1")

        val error = personRepository(stack).deleteAccount("000000").exceptionOrNull()

        assertEquals("验证码错误或已失效", error?.message)
        assertEquals("t1", stack.sessionStore.cachedToken)
    }

    @Test
    fun adminDeleteSendsUserIdOnly() = runTest {
        val stack = OaaTestStack { json("""{"code":200,"message":"success","data":null}""") }

        UserManagementRepositoryImpl(OaaUserApi(stack.requester)).deleteUser(42).getOrThrow()

        val body = testJson.parseToJsonElement(stack.requestsTo("/user/delete").single().bodyText()).jsonObject
        assertEquals("42", body["user_id"]?.jsonPrimitive?.content)
        assertFalse("code" in body)
    }

    @Test
    fun cancelDeleteAccountSendsCancelSceneCodeAndRestoresWithoutClearingSession() = runTest {
        val stack = OaaTestStack { request ->
            when (request.url.encodedPath) {
                "/v2/user/me" -> json(meBody)
                else -> json("""{"code":200,"message":"success","data":null}""")
            }
        }
        stack.sessionStore.saveTokens("t1", "r1")
        stack.sessionStore.saveUserId(7)
        val repo = personRepository(stack)

        repo.sendCancelDeleteCode().getOrThrow()
        repo.cancelDeleteAccount("020247").getOrThrow()

        val send = testJson.parseToJsonElement(stack.requestsTo("/auth/send").single().bodyText()).jsonObject
        assertEquals("cancel_delete", send["scene"]?.jsonPrimitive?.content)
        assertEquals("zhangsan", send["account"]?.jsonPrimitive?.content)
        val cancel = stack.requestsTo("/user/delete/cancel").single()
        assertEquals("Bearer t1", cancel.headers["Authorization"])
        val body = testJson.parseToJsonElement(cancel.bodyText()).jsonObject
        assertEquals("020247", body["code"]?.jsonPrimitive?.content)
        assertEquals("t1", stack.sessionStore.cachedToken)
        assertTrue(stack.requestsTo("/auth/logout").isEmpty())
    }

    @Test
    fun uploadFilePostsMultipartWithSceneAndStringCode() = runTest {
        val stack = OaaTestStack {
            json("""{"code":"200","message":null,"data":{"uri":"bucket/doc.pdf","url":"signed-url-doc"}}""")
        }
        val repo = personRepository(stack)

        val stored = repo.uploadFile(byteArrayOf(9, 8, 7), "doc.pdf", "application/pdf").getOrThrow()

        assertEquals("bucket/doc.pdf", stored.uri)
        assertEquals("signed-url-doc", stored.url)
        val req = stack.requestsTo("/upload/file").single()
        assertTrue(req.body.contentType.toString().startsWith("multipart/form-data"))
    }

    @Test
    fun checkServerHealthProbesLivezAndHealthz() = runTest {
        var readyHealthy = true
        val stack = OaaTestStack { request ->
            when (request.url.encodedPath) {
                "/v2/livez" -> json("""{"code":200,"message":"success","data":null}""")
                "/v2/healthz" ->
                    if (readyHealthy) json("""{"code":200,"message":"success","data":null}""")
                    else json("""{"code":500,"message":"db down","data":null}""", HttpStatusCode.InternalServerError)
                else -> error("unexpected ${request.url}")
            }
        }
        val repo = personRepository(stack)

        val ok = repo.checkServerHealth().getOrThrow()
        assertTrue(ok.isHealthy)
        assertTrue(ok.live && ok.ready)

        readyHealthy = false
        val degraded = repo.checkServerHealth().getOrThrow()
        assertFalse(degraded.isHealthy)
        assertTrue(degraded.live)
        assertFalse(degraded.ready)
    }

    @Test
    fun userListIsAllOmitsPaginationAndBatchUpdateParsesFailures() = runTest {
        val stack = OaaTestStack { request ->
            when (request.url.encodedPath) {
                "/v2/user/list" -> json(
                    """{"code":200,"message":"success","data":{"total":1,"list":[
                    {"user_id":12,"student_id":"20240012","username":"lisi","name":"李四","department":"秘书处","role":"干事"}
                    ]}}"""
                )
                "/v2/user/batch" -> json(
                    """{"code":200,"message":"success","data":[
                    {"id":13,"student_id":"20240013","username":"wangwu","name":"王五","error_message":"权限不足"}
                    ]}"""
                )
                else -> error("unexpected ${request.url}")
            }
        }
        val repo = UserManagementRepositoryImpl(OaaUserApi(stack.requester))

        val page = repo.listUsers(UserQuery(keyword = "李", isAll = true)).getOrThrow()
        assertEquals(1, page.list.size)
        val params = stack.requestsTo("/user/list").single().url.parameters
        assertEquals("true", params["is_all"])
        assertEquals("李", params["keyword"])
        assertNull(params["page"])
        assertNull(params["page_size"])

        val failures = repo.batchUpdate(
            listOf(
                UserBatchItem(userId = 12, departmentId = 1, roleId = 4),
                UserBatchItem(userId = 13, departmentId = 2, roleId = 5)
            )
        ).getOrThrow()
        assertEquals(1, failures.size)
        assertEquals(13, failures.single().id)
        assertEquals("权限不足", failures.single().errorMessage)
        val batchBody = testJson.parseToJsonElement(stack.requestsTo("/user/batch").single().bodyText()).jsonArray
        assertEquals(2, batchBody.size)
    }

    @Test
    fun recruitmentTermFilterFillableDepartmentsAndInterviewerLifecycle() = runTest {
        val stack = OaaTestStack { request ->
            when (request.url.encodedPath) {
                "/v2/term/list" -> json("""{"code":200,"message":"success","data":[{"term_id":5,"year":2026,"type":"招新","title":"秋招"}]}""")
                "/v2/application/department" -> json(
                    """{"code":200,"message":"success","data":[{"id":2,"name":"算法竞赛部","type":"部门","is_active":true}]}"""
                )
                "/v2/interviewer/list" -> json(
                    """{"code":200,"message":"success","data":[{"interviewer_id":9,"term_id":5,"user_id":12,"name":"李四","department":"算法竞赛部","role":"部长","remark":"主面"}]}"""
                )
                "/v2/interviewer/update" -> json("""{"code":200,"message":"success","data":"更新成功"}""")
                "/v2/interviewer/delete" -> json("""{"code":200,"message":"success","data":null}""")
                else -> error("unexpected ${request.url}")
            }
        }
        val repo = RecruitmentRepositoryImpl(OaaRecruitmentApi(stack.requester), testJson)

        val terms = repo.getTerms(year = 2026, type = "招新").getOrThrow()
        assertEquals(5, terms.single().resolvedId)
        val termParams = stack.requestsTo("/term/list").single().url.parameters
        assertEquals("2026", termParams["year"])
        assertEquals("招新", termParams["type"])

        val departments = repo.getFillableDepartments(roleId = 6).getOrThrow()
        assertEquals(listOf(2), departments.map { it.id })
        assertEquals("6", stack.requestsTo("/application/department").single().url.parameters["role_id"])

        val interviewers = repo.getInterviewers(termId = 5).getOrThrow()
        assertEquals(9, interviewers.single().resolvedId)
        assertEquals("李四", interviewers.single().displayName)
        assertEquals("5", stack.requestsTo("/interviewer/list").single().url.parameters["term_id"])

        repo.updateInterviewer(interviewerId = 9, remark = "二面").getOrThrow()
        val updateBody = testJson.parseToJsonElement(stack.requestsTo("/interviewer/update").single().bodyText()).jsonObject
        assertEquals("9", updateBody["interviewer_id"]?.jsonPrimitive?.content)
        assertEquals("二面", updateBody["remark"]?.jsonPrimitive?.content)

        repo.deleteInterviewer(interviewerId = 9).getOrThrow()
        val deleteBody = testJson.parseToJsonElement(stack.requestsTo("/interviewer/delete").single().bodyText()).jsonObject
        assertEquals("9", deleteBody["interviewer_id"]?.jsonPrimitive?.content)
    }

    @Test
    fun uploadApplicationAvatarPostsApplicationScene() = runTest {
        val stack = OaaTestStack {
            json("""{"code":200,"message":"success","data":{"uri":"application/202610.jpg","url":"https://oss/app.jpg"}}""")
        }
        val repo = RecruitmentRepositoryImpl(
            api = OaaRecruitmentApi(stack.requester),
            json = testJson,
            userApi = OaaUserApi(stack.requester)
        )

        val stored = repo.uploadApplicationAvatar(byteArrayOf(0xF.toByte(), 0xA.toByte())).getOrThrow()

        assertEquals("application/202610.jpg", stored.uri)
        assertEquals("https://oss/app.jpg", stored.url)
        val req = stack.requestsTo("/upload/image").single()
        assertTrue(req.body.contentType.toString().startsWith("multipart/form-data"))
        val rawBody = req.body.toByteArray().decodeToString()
        assertTrue("application" in rawBody, "应携带 scene=application")
    }

    @Test
    fun storedFileParsesObjectPlainStringAndNull() {
        val objApp = testJson.decodeFromString<Application>(
            """{"id":1,"avatar":{"uri":"application/a.jpg","url":"https://cdn/a.jpg"}}"""
        )
        assertEquals("application/a.jpg", objApp.avatar.uri)
        assertEquals("https://cdn/a.jpg", objApp.avatar.url)
        assertEquals("application/a.jpg", objApp.toForm().avatarUri)
        assertEquals("https://cdn/a.jpg", objApp.toForm().avatarUrl)

        val uriApp = testJson.decodeFromString<Application>(
            """{"id":2,"avatar":"application/only-uri.jpg"}"""
        )
        assertEquals("application/only-uri.jpg", uriApp.avatar.uri)
        assertEquals("", uriApp.avatar.url)

        val urlApp = testJson.decodeFromString<Application>(
            """{"id":3,"avatar":"https://cdn/only-url.jpg"}"""
        )
        assertEquals("", urlApp.avatar.uri, "展示 URL 不可作为提交的 OSS 相对路径")
        assertEquals("https://cdn/only-url.jpg", urlApp.avatar.url)

        val nullApp = testJson.decodeFromString<Application>("""{"id":4,"avatar":null}""")
        assertEquals("", nullApp.avatar.uri)
        assertEquals("", nullApp.avatar.url)
    }

    @Test
    fun gradeFlowsThroughPersonUpdateAvatarUploadAndUserListFilter() = runTest {
        val stack = OaaTestStack { request ->
            when (request.url.encodedPath) {
                "/v2/user/me" -> json(
                    """{"code":200,"message":"success","data":{"user_id":7,"student_id":"24101010330",
                    "username":"zhangsan","name":"张三","email":"z@x.com","grade":2024,
                    "avatar":{"uri":"avatar/old.jpg","url":"https://oss/old.jpg"}}}"""
                )
                "/v2/user/me/update" -> json("""{"code":200,"message":"success","data":null}""")
                "/v2/upload/image" -> json(
                    """{"code":200,"message":"success","data":{"uri":"avatar/new.jpg","url":"https://oss/new.jpg"}}"""
                )
                "/v2/user/list" -> json(
                    """{"code":200,"message":"success","data":{"total":1,"list":[
                    {"user_id":7,"student_id":"24101010330","username":"zhangsan","name":"张三","grade":2024,"department":"技术部","role":"部长"}
                    ]}}"""
                )
                else -> error("unexpected ${request.url}")
            }
        }
        val personRepo = personRepository(stack)
        val userManageRepo = UserManagementRepositoryImpl(OaaUserApi(stack.requester))

        val me = personRepo.getPersonInfo().getOrThrow()
        assertEquals(2024, me.resolvedGrade)

        personRepo.updateProfile(username = "zhangsan2", email = "z2@x.com", grade = 2025).getOrThrow()
        personRepo.uploadAvatar(byteArrayOf(1, 2, 3)).getOrThrow()

        val updates = stack.requestsTo("/user/me/update")
        assertEquals(2, updates.size)
        val profileUpdate = testJson.parseToJsonElement(updates[0].bodyText()).jsonObject
        assertEquals("2025", profileUpdate["grade"]?.jsonPrimitive?.content)
        assertEquals("avatar/old.jpg", profileUpdate["avatar"]?.jsonPrimitive?.content)

        val avatarUpdate = testJson.parseToJsonElement(updates[1].bodyText()).jsonObject
        assertEquals("2024", avatarUpdate["grade"]?.jsonPrimitive?.content, "仅上传头像时应保留原年级")
        assertEquals("avatar/new.jpg", avatarUpdate["avatar"]?.jsonPrimitive?.content)

        val page = userManageRepo.listUsers(UserQuery(grade = 2024, page = 1, pageSize = 20)).getOrThrow()
        assertEquals(2024, page.list.single().resolvedGrade)
        assertEquals("2024", stack.requestsTo("/user/list").single().url.parameters["grade"])
    }

    @Test
    fun nonJsonServerErrorBecomesHttpError() = runTest {
        val stack = OaaTestStack { respondError(HttpStatusCode.BadGateway, "<html>502 Bad Gateway</html>") }

        val error = (OaaUserApi(stack.requester).me().exceptionOrNull() as AppException).error

        assertIs<AppError.Http>(error)
        assertEquals(502, error.status)
    }
}
