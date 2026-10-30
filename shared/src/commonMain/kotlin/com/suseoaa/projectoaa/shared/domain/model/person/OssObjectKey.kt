package com.suseoaa.projectoaa.shared.domain.model.person

/**
 * OSS 对象键，如 `application/photo.jpg`。接口返回的 uri 才是对象键；
 * 下载 URL 可能包含桶名、签名等信息，不能通过截取 URL 路径来猜测对象键。
 */
object OssObjectKey {
    private val scheme = Regex("^[A-Za-z][A-Za-z0-9+.-]*:")

    fun parse(value: String): String? {
        val key = value.trim()
        return key.takeIf {
            it.isNotEmpty() && !it.startsWith('/') && !scheme.containsMatchIn(it) &&
                it.none { char -> char == '\\' || char == '?' || char == '#' || char.isISOControl() }
        }
    }
}
