package com.suseoaa.projectoaa.shared.data.local.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * v1 遗留：OAA 账号的明文口令。
 *
 * v1 后端没有 refresh token，只能把密码存下来每 10 天重新登录一次。v2 改用
 * refresh token 保活后不再写入，这里只保留 [clear]，用于升级后和退出登录时
 * 把老版本存下的明文密码删掉。
 */
class CredentialStore(private val dataStore: DataStore<Preferences>) {

    /** 由 UserDataCleaner 调用；internal 会被模块边界挡住，故为 public。 */
    suspend fun clear() {
        dataStore.edit { it.remove(Keys.USER_PASSWORD) }
    }

    private object Keys {
        val USER_PASSWORD = stringPreferencesKey("user_password")
    }
}
