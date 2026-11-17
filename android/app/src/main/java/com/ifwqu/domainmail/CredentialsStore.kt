package com.ifwqu.domainmail

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** 登录页「记住账号密码」勾选后保存的账号密码。 */
data class SavedCredentials(
    val baseUrl: String,
    val email: String,
    val password: String,
)

/**
 * 账号密码只在用户勾选「记住账号密码」时保存，存在 EncryptedSharedPreferences 里，
 * 密钥由 Android Keystore 保管，文件本身是加密的。不勾选时什么都不存。
 * Keystore 不可用时直接当作未记住：不写明文、不抛异常、不影响登录流程。
 */
class CredentialsStore(context: Context) {
    private val appContext = context.applicationContext

    private fun prefs(): SharedPreferences? {
        return try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                "muse_email_credentials",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            null
        }
    }

    fun load(): SavedCredentials? {
        val p = prefs() ?: return null
        return try {
            val email = p.getString("email", "") ?: ""
            val password = p.getString("password", "") ?: ""
            if (email.isBlank() || password.isBlank()) return null
            SavedCredentials(
                baseUrl = p.getString("baseUrl", "") ?: "",
                email = email,
                password = password,
            )
        } catch (e: Exception) {
            null
        }
    }

    fun save(baseUrl: String, email: String, password: String) {
        try {
            prefs()?.edit()
                ?.putString("baseUrl", baseUrl)
                ?.putString("email", email)
                ?.putString("password", password)
                ?.apply()
        } catch (e: Exception) {
            // 存不进就当没记住，不影响登录
        }
    }

    fun clear() {
        try {
            prefs()?.edit()?.clear()?.apply()
        } catch (e: Exception) {
            // 清不掉也不影响登录
        }
    }
}
