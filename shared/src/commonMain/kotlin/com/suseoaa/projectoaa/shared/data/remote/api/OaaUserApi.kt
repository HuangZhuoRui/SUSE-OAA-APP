package com.suseoaa.projectoaa.shared.data.remote.api

import com.suseoaa.projectoaa.shared.data.remote.ApiConfig
import com.suseoaa.projectoaa.shared.domain.model.person.DeleteUserRequest
import com.suseoaa.projectoaa.shared.domain.model.person.PersonData
import com.suseoaa.projectoaa.shared.domain.model.person.StoredFile
import com.suseoaa.projectoaa.shared.domain.model.person.UpdateMeRequest
import com.suseoaa.projectoaa.shared.domain.model.person.UserBatchFailure
import com.suseoaa.projectoaa.shared.domain.model.person.UserBatchItem
import com.suseoaa.projectoaa.shared.domain.model.person.UserPage
import com.suseoaa.projectoaa.shared.domain.model.person.UserQuery
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

/** `/user` 与 `/upload` 下的接口。 */
class OaaUserApi(private val requester: OaaRequester) {

    suspend fun me(): Result<PersonData> =
        requester.get("/user/me", "获取个人信息失败")

    suspend fun updateMe(request: UpdateMeRequest): Result<Unit> =
        requester.post("/user/me/update", request, "更新个人信息失败")

    suspend fun list(query: UserQuery): Result<UserPage?> =
        requester.get(
            "/user/list",
            "获取成员列表失败",
            params = mapOf(
                "keyword" to query.keyword.ifBlank { null },
                "department" to query.department.ifBlank { null },
                "role" to query.role.ifBlank { null },
                "page" to query.page,
                "page_size" to query.pageSize
            )
        )

    suspend fun batchUpdate(items: List<UserBatchItem>): Result<List<UserBatchFailure>?> =
        requester.post("/user/batch", items, "批量调整失败")

    suspend fun delete(request: DeleteUserRequest): Result<Unit> =
        requester.post("/user/delete", request, "删除用户失败")

    /** 图片上传，[scene] 目前只定义了 `avatar`。限 jpg / png / gif / webp / avif，最大 4MB。 */
    suspend fun uploadImage(scene: String, bytes: ByteArray, fileName: String, mimeType: String): Result<StoredFile> =
        upload("/upload/image", scene, bytes, fileName, mimeType, "图片上传失败")

    /** 通用文件上传，最大 100MB。 */
    suspend fun uploadFile(scene: String, bytes: ByteArray, fileName: String, mimeType: String): Result<StoredFile> =
        upload("/upload/file", scene, bytes, fileName, mimeType, "文件上传失败")

    private suspend fun upload(
        path: String,
        scene: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
        fallbackMessage: String
    ): Result<StoredFile> = requester.execute(fallbackMessage, { requester.decode<StoredFile>(it) }) {
        requester.client.submitFormWithBinaryData(
            url = ApiConfig.OAA_BASE + path,
            formData = formData {
                append("scene", scene)
                append("file", bytes, Headers.build {
                    append(HttpHeaders.ContentType, mimeType)
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                })
            }
        )
    }
}
