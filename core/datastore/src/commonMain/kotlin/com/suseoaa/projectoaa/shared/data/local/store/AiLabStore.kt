package com.suseoaa.projectoaa.shared.data.local.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * AI 实验室：端侧模型选择、推理后端偏好，以及模型下载相关的凭据与 ETag。
 */
class AiLabStore(private val dataStore: DataStore<Preferences>) {

    val selectedModelIdFlow: Flow<String?> = dataStore.data.map { it[Keys.SELECTED_MODEL_ID] }

    suspend fun saveSelectedModelId(modelId: String) {
        dataStore.edit { it[Keys.SELECTED_MODEL_ID] = modelId }
    }

    val preferGpuFlow: Flow<Boolean> = dataStore.data.map { it[Keys.PREFER_GPU] ?: true }

    suspend fun savePreferGpu(preferGpu: Boolean) {
        dataStore.edit { it[Keys.PREFER_GPU] = preferGpu }
    }

    val kaggleAuthFlow: Flow<String?> = dataStore.data.map { it[Keys.KAGGLE_AUTH_TOKEN] }

    suspend fun saveKaggleAuth(authBase64: String) {
        dataStore.edit { it[Keys.KAGGLE_AUTH_TOKEN] = authBase64 }
    }

    /** 模型文件的 ETag，用于断点续传与增量校验；每个模型一把键。 */
    fun modelETagFlow(modelId: String): Flow<String?> =
        dataStore.data.map { it[modelETagKey(modelId)] }

    suspend fun saveModelETag(modelId: String, etag: String) {
        dataStore.edit { it[modelETagKey(modelId)] = etag }
    }

    private fun modelETagKey(modelId: String) = stringPreferencesKey("etag_$modelId")

    private object Keys {
        val SELECTED_MODEL_ID = stringPreferencesKey("ailab_selected_model_id")
        val PREFER_GPU = booleanPreferencesKey("ailab_prefer_gpu")
        val KAGGLE_AUTH_TOKEN = stringPreferencesKey("kaggle_auth_token")
    }
}
