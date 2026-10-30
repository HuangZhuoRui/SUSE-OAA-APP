package com.suseoaa.projectoaa.shared.data.remote

import com.suseoaa.projectoaa.shared.data.local.store.CredentialStore
import com.suseoaa.projectoaa.shared.data.local.store.UserDataCleaner
import com.suseoaa.projectoaa.shared.data.local.store.UserProfileStore
import com.suseoaa.projectoaa.shared.data.remote.api.OaaAuthApi
import com.suseoaa.projectoaa.shared.data.remote.api.OaaOrganizationApi
import com.suseoaa.projectoaa.shared.data.remote.api.OaaRecruitmentApi
import com.suseoaa.projectoaa.shared.data.remote.api.OaaUserApi
import com.suseoaa.projectoaa.shared.data.repository.OrganizationRepositoryImpl
import com.suseoaa.projectoaa.shared.data.repository.PersonRepositoryImpl
import com.suseoaa.projectoaa.shared.data.repository.RecruitmentRepositoryImpl
import com.suseoaa.projectoaa.shared.domain.model.person.OssObjectKey
import com.suseoaa.projectoaa.shared.domain.model.person.StoredFile
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Application
import com.suseoaa.projectoaa.shared.domain.model.recruitment.ApplicationForm
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApplicationPhotoContractTest {
    // 下载路径含桶名和查询签名，对象键不含桶名和签名。
    private val signedUrl = URLBuilder().apply {
        protocol = URLProtocol.HTTPS
        host = "storage.invalid"
        encodedPath = "/oaa-img/application/photo.jpg"
        parameters.append("X-Amz-Signature", "test-signature")
    }.buildString()

    @Test
    fun uploadCreateAndUpdateKeepTheReturnedKeyWithoutBucketOrSignedUrl() = runTest {
        for (key in listOf("application/photo.jpg", "avatar/photo.jpg")) {
            val stack = OaaTestStack { request ->
                if (request.url.encodedPath == "/v2/upload/image") {
                    json(uploadResponse(buildJsonObject {
                        put("uri", key)
                        put("url", signedUrl)
                    }))
                } else {
                    json("""{"code":200,"message":"success","data":null}""")
                }
            }
            val repo = RecruitmentRepositoryImpl(OaaRecruitmentApi(stack.requester), testJson, OaaUserApi(stack.requester))
            val stored = repo.uploadApplicationAvatar(byteArrayOf(1, 2, 3)).getOrThrow()
            val form = ApplicationForm(avatarUri = stored.uri, avatarUrl = stored.url)
            repo.createApplication(1, form).getOrThrow()
            repo.updateApplication(1, Application(applicationId = 7), form).getOrThrow()

            assertEquals(key, stored.uri)
            for (path in listOf("/application/create", "/application/update")) {
                val body = testJson.parseToJsonElement(stack.requestsTo(path).single().bodyText()).jsonObject
                assertEquals(key, body["avatar"]?.jsonPrimitive?.content)
                assertFalse(body["avatar"]?.jsonPrimitive?.content.orEmpty().contains("X-Amz"))
            }
        }
    }

    @Test
    fun uploadCannotSucceedWithoutAUsableRelativeKey() = runTest {
        val invalidData = listOf(
            JsonPrimitive(signedUrl),
            buildJsonObject { put("url", signedUrl) },
            buildJsonObject { put("uri", signedUrl); put("url", signedUrl) },
            buildJsonObject { put("uri", "/oaa-img/application/photo.jpg") },
            buildJsonObject { put("uri", "application/photo.jpg?X-Amz-Signature=test") },
            buildJsonObject { put("uri", "") }
        )
        for (data in invalidData) {
            val stack = OaaTestStack { json(uploadResponse(data)) }
            val result = OaaUserApi(stack.requester).uploadImage("application", byteArrayOf(1), "photo.jpg", "image/jpeg")
            assertTrue(result.isFailure, data.toString())
            assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("重新上传"))
        }
    }

    @Test
    fun createAndUpdateRejectDisplayUrlsEvenIfAnOlderPhotoExists() = runTest {
        val stack = OaaTestStack { error("无效照片路径不得发起提交请求") }
        val repo = RecruitmentRepositoryImpl(OaaRecruitmentApi(stack.requester), testJson)
        val existing = Application(avatar = StoredFile(uri = "application/old.jpg"))
        for (path in listOf(signedUrl, "/oaa-img/application/photo.jpg", "file:/tmp/photo.jpg", "content:photo")) {
            val form = ApplicationForm(avatarUri = path, avatarUrl = signedUrl)
            assertTrue(repo.createApplication(1, form).isFailure)
            assertTrue(repo.updateApplication(1, existing, form).isFailure)
        }
        assertTrue(stack.requests.isEmpty())
    }

    @Test
    fun urlOnlyLegacyPhotoCanBePreviewedButCannotBeResubmitted() = runTest {
        for (avatar in listOf(
            JsonPrimitive(signedUrl),
            buildJsonObject { put("uri", signedUrl) },
            buildJsonObject { put("url", signedUrl) }
        )) {
            val application = testJson.decodeFromString<Application>(buildJsonObject {
                put("id", 7)
                put("avatar", avatar)
            }.toString())
            assertEquals("", application.avatar.uri)
            assertEquals(signedUrl, application.toForm().avatarUrl)
            val stack = OaaTestStack { error("展示 URL 不应提交") }
            val repo = RecruitmentRepositoryImpl(OaaRecruitmentApi(stack.requester), testJson)
            assertTrue(repo.updateApplication(1, application, application.toForm()).isFailure)
            assertTrue(stack.requests.isEmpty())
        }
    }

    @Test
    fun profileUpdateDoesNotSilentlyEraseAnUnrecoverableLegacyAvatar() = runTest {
        val stack = OaaTestStack { request ->
            assertEquals("/v2/user/me", request.url.encodedPath)
            json(uploadResponse(buildJsonObject {
                put("username", "user")
                put("email", "user@example.invalid")
                put("avatar", signedUrl)
            }))
        }
        val repo = PersonRepositoryImpl(
            OaaUserApi(stack.requester), OaaAuthApi(stack.requester),
            OrganizationRepositoryImpl(OaaOrganizationApi(stack.requester)),
            UserDataCleaner(stack.sessionStore, CredentialStore(InMemoryPreferences()), UserProfileStore(InMemoryPreferences())) {}
        )
        assertTrue(repo.updateProfile("user2", "user@example.invalid").isFailure)
        assertTrue(stack.requestsTo("/user/me/update").isEmpty())
    }

    @Test
    fun relativeKeysRemainRelativeAndAreNeverDerivedFromDownloadUrls() {
        assertEquals("avatar/photo.jpg", OssObjectKey.parse("avatar/photo.jpg"))
        assertEquals("application/photo.jpg", OssObjectKey.parse("application/photo.jpg"))
        assertNull(OssObjectKey.parse(signedUrl))
        assertNull(OssObjectKey.parse("//storage.invalid/oaa-img/photo.jpg"))
        assertNull(OssObjectKey.parse("application/photo.jpg#fragment"))
        assertNull(OssObjectKey.parse("C:\\photos\\photo.jpg"))
    }

    private fun uploadResponse(data: JsonElement) = buildJsonObject {
        put("code", "200")
        put("message", "success")
        put("data", data)
    }.toString()
}
