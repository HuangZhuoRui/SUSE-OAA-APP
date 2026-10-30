package com.suseoaa.projectoaa.shared.data.remote

import com.suseoaa.projectoaa.shared.data.remote.api.OaaAuthApi
import com.suseoaa.projectoaa.shared.data.remote.api.OaaRecruitmentApi
import com.suseoaa.projectoaa.shared.data.remote.api.OaaUserApi
import com.suseoaa.projectoaa.shared.data.repository.OaaRegisterRepositoryImpl
import com.suseoaa.projectoaa.shared.data.repository.RecruitmentRepositoryImpl
import com.suseoaa.projectoaa.shared.domain.model.person.PersonData
import com.suseoaa.projectoaa.shared.domain.model.person.UserBatchItem
import com.suseoaa.projectoaa.shared.domain.model.person.UserListItem
import com.suseoaa.projectoaa.shared.domain.model.person.UserQuery
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OaaGraduationContractTest {
    @Test
    fun registrationSendsNumericGradeAndRejectsInvalidYearsBeforeRequest() = runTest {
        val stack = OaaTestStack { json("""{"code":200,"message":"success","data":null}""") }
        val repo = OaaRegisterRepositoryImpl(OaaAuthApi(stack.requester))

        for (grade in listOf(0, 1999, 2000, 10000)) {
            assertTrue(repo.register("24101010330", "张三", "zhangsan", "password", "z@x.com", grade).isFailure)
        }
        assertTrue(stack.requests.isEmpty())
        repo.register("24101010330", "张三", "zhangsan", "password", "z@x.com", 2024).getOrThrow()

        val body = testJson.parseToJsonElement(stack.requestsTo("/auth/register").single().bodyText()).jsonObject
        assertEquals(JsonPrimitive(2024), body["grade"])
    }

    @Test
    fun graduationFlagsDistinguishMissingFalseAndTrue() {
        for (value in listOf(false, true)) {
            val body = """{"user_id":7,"grade":2024,"is_graduated":$value}"""
            assertEquals(value, testJson.decodeFromString<PersonData>(body).isGraduated)
            assertEquals(value, testJson.decodeFromString<UserListItem>(body).isGraduated)
        }
        assertNull(testJson.decodeFromString<PersonData>("""{"user_id":7}""").isGraduated)
        assertNull(testJson.decodeFromString<UserListItem>("""{"user_id":7}""").isGraduated)
    }

    @Test
    fun userListPreservesFalseFilterAndOmitsItWhenUnspecified() = runTest {
        val stack = OaaTestStack { json("""{"code":200,"message":"success","data":{"total":0,"list":[]}}""") }
        val api = OaaUserApi(stack.requester)
        api.list(UserQuery(grade = 2024, isGraduated = false, isAll = true)).getOrThrow()
        api.list(UserQuery(isGraduated = true)).getOrThrow()
        api.list(UserQuery()).getOrThrow()

        val requests = stack.requestsTo("/user/list")
        assertEquals("false", requests[0].url.parameters["is_graduated"])
        assertEquals("2024", requests[0].url.parameters["grade"])
        assertNull(requests[0].url.parameters["page"])
        assertEquals("true", requests[1].url.parameters["is_graduated"])
        assertNull(requests[2].url.parameters["is_graduated"])
    }

    @Test
    fun batchSendsOnlyExplicitGradeAndNeverSendsGraduationFlag() = runTest {
        val stack = OaaTestStack { json("""{"code":200,"message":"success","data":[]}""") }
        OaaUserApi(stack.requester).batchUpdate(listOf(
            UserBatchItem(1, 2, 3, grade = 2024),
            UserBatchItem(2, 2, 3)
        )).getOrThrow()

        val body = testJson.parseToJsonElement(stack.requestsTo("/user/batch").single().bodyText()).jsonArray
        assertEquals(JsonPrimitive(2024), body[0].jsonObject["grade"])
        assertFalse("grade" in body[1].jsonObject)
        body.forEach { assertFalse("is_graduated" in it.jsonObject) }
    }

    @Test
    fun applicationGraduationUsesExplicitFlagThenMemberLookupAndKeepsUnknowns() = runTest {
        val stack = OaaTestStack { request ->
            when (request.url.encodedPath) {
                "/v2/application/list" -> json("""{"code":200,"message":"success","data":[
                    {"id":1,"user_id":10},
                    {"id":2,"user_id":11,"is_graduated":false},
                    {"id":3,"user_id":12},
                    {"id":4,"student_id":"20240004"}
                ]}""")
                "/v2/user/list" -> json("""{"code":200,"message":"success","data":{"total":3,"list":[
                    {"user_id":10,"is_graduated":true},
                    {"user_id":11,"is_graduated":true},
                    {"user_id":14,"student_id":"20240004","is_graduated":false}
                ]}}""")
                else -> error("unexpected ${request.url}")
            }
        }
        val repo = RecruitmentRepositoryImpl(OaaRecruitmentApi(stack.requester), testJson, OaaUserApi(stack.requester))

        assertEquals(listOf(true, false, null, false), repo.getApplications(1, null).getOrThrow().map { it.isGraduated })
        assertEquals("true", stack.requestsTo("/user/list").single().url.parameters["is_all"])
        assertNull(stack.requestsTo("/user/list").single().url.parameters["is_graduated"])
    }

    @Test
    fun memberLookupFailureDoesNotHideApplications() = runTest {
        val stack = OaaTestStack { request ->
            when (request.url.encodedPath) {
                "/v2/application/list" -> json("""{"code":200,"message":"success","data":[{"id":1,"user_id":10}]}""")
                else -> json("""{"code":403,"message":"权限不足","data":null}""", HttpStatusCode.Forbidden)
            }
        }
        val repo = RecruitmentRepositoryImpl(OaaRecruitmentApi(stack.requester), testJson, OaaUserApi(stack.requester))

        val application = repo.getApplications(1, null).getOrThrow().single()
        assertEquals(1, application.resolvedId)
        assertNull(application.isGraduated)
    }

    @Test
    fun knownGraduationFlagsDoNotRequireMemberListAccess() = runTest {
        val stack = OaaTestStack { request ->
            assertEquals("/v2/application/list", request.url.encodedPath)
            json("""{"code":200,"message":"success","data":[{"id":1,"is_graduated":true}]}""")
        }
        val repo = RecruitmentRepositoryImpl(OaaRecruitmentApi(stack.requester), testJson, OaaUserApi(stack.requester))
        assertEquals(true, repo.getApplications(1, null).getOrThrow().single().isGraduated)
        assertTrue(stack.requestsTo("/user/list").isEmpty())
    }
}
