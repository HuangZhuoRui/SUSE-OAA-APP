package com.suseoaa.projectoaa.shared.data.remote.api

import com.suseoaa.projectoaa.shared.data.remote.ApiConfig
import com.suseoaa.projectoaa.shared.domain.model.person.CancelDeleteRequest
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

/** `/user`、`/upload` 与基础设施探针接口。 */
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
                "grade" to query.grade?.takeIf { it > 0 },
                "page" to query.page.takeUnless { query.isAll == true },
                "page_size" to query.pageSize.takeUnless { query.isAll == true },
                "is_all" to query.isAll
            )
        )

    suspend fun batchUpdate(items: List<UserBatchItem>): Result<List<UserBatchFailure>?> =
        requester.post("/user/batch", items, "批量调整失败")

    /** 管理员删除成员，或用户凭验证码注销自己，见 [DeleteUserRequest]。 */
    suspend fun delete(request: DeleteUserRequest, fallbackMessage: String = "删除用户失败"): Result<Unit> =
        requester.post("/user/delete", request, fallbackMessage)

    /** 冷静期内撤销注销。 */
    suspend fun cancelDelete(request: CancelDeleteRequest): Result<Unit> =
        requester.post("/user/delete/cancel", request, "撤销注销失败")

    /** 图片上传，[scene] 支持 `avatar`（用户头像）与 `application`（招新换届申请表照片）。限 jpg / png / gif / webp / avif，最大 4MB。 */
    suspend fun uploadImage(scene: String, bytes: ByteArray, fileName: String, mimeType: String): Result<StoredFile> =
        upload("/upload/image", scene, bytes, fileName, mimeType, "图片上传失败")

    /** 通用文件上传，最大 100MB。 */
    suspend fun uploadFile(scene: String, bytes: ByteArray, fileName: String, mimeType: String): Result<StoredFile> =
        upload("/upload/file", scene, bytes, fileName, mimeType, "文件上传失败")

    /** 服务存活探针（GET /livez）。 */
    suspend fun livez(): Result<Unit> =
        requester.get("/livez", "服务存活检测失败")

    /** 服务就绪探针（GET /healthz）。 */
    suspend fun healthz(): Result<Unit> =
        requester.get("/healthz", "服务就绪检测失败")

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
