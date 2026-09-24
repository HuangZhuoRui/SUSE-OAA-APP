package com.suseoaa.projectoaa.shared.data.remote.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement

// 文档没写响应结构的接口，data 可能是数组、单个对象，或是包在 list 之类字段里的数组。
// 这几个函数把这些形态统一成列表，等后端文档补齐后可以换回严格解析。

private val LIST_KEYS = listOf("list", "items", "records", "data")

private fun JsonObject.nestedArray(): JsonArray? =
    LIST_KEYS.firstNotNullOfOrNull { this[it] as? JsonArray }

@PublishedApi
internal fun JsonElement?.asElementList(): List<JsonElement> = when (this) {
    is JsonArray -> this
    is JsonObject -> nestedArray() ?: listOf(this)
    else -> emptyList()
}

inline fun <reified T> Json.decodeListLenient(element: JsonElement?): List<T> =
    element.asElementList().map { decodeFromJsonElement<T>(it) }

/** 取字符串列表：元素可以是字符串，也可以是带 name / value / decision / label 字段的对象。 */
fun JsonElement?.toStringListLenient(): List<String> {
    val items = when (this) {
        is JsonArray -> this
        is JsonObject -> nestedArray() ?: values.firstOrNull { it is JsonArray } as? JsonArray ?: return emptyList()
        else -> return emptyList()
    }
    return items.mapNotNull { item ->
        when (item) {
            is JsonPrimitive -> item.contentOrNull
            is JsonObject -> listOf("name", "value", "decision", "label").firstNotNullOfOrNull { key ->
                (item[key] as? JsonPrimitive)?.contentOrNull
            }
            else -> null
        }?.takeIf { it.isNotBlank() }
    }
}
