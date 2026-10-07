package com.cfadmin.pro.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Almacen seguro del token y metadatos de la cuenta.
 *
 * Intenta usar EncryptedSharedPreferences. Si el Keystore falla
 * (clave invalidada, dispositivo reciente, reinstalacion, etc.),
 * cae a SharedPreferences normales para NO crashear la app.
 */
class TokenStore(context: Context) {

    private val prefs: SharedPreferences = createPrefs(context)

    private companion object {
        const val SECURE = "cf_secure_prefs"
        const val FALLBACK = "cf_fallback_prefs"
        const val KEY_TOKEN = "api_token"
        const val KEY_ACCOUNT = "account_id"
        const val KEY_ACCOUNT_NAME = "account_name"
        const val TAG = "TokenStore"
    }

    private fun createPrefs(ctx: Context): SharedPreferences {
        try {
            val masterKey = MasterKey.Builder(ctx)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                ctx,
                SECURE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Throwable) {
            Log.w(TAG, "EncryptedSharedPreferences fallo, usando fallback: ${e.message}")
        }

        try {
            ctx.deleteSharedPreferences(SECURE)
            val masterKey = MasterKey.Builder(ctx)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                ctx,
                SECURE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Throwable) {
            Log.w(TAG, "Reintento EncryptedSharedPreferences tambien fallo: ${e.message}")
        }

        return ctx.getSharedPreferences(FALLBACK, Context.MODE_PRIVATE)
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? = try {
        prefs.getString(KEY_TOKEN, null)
    } catch (e: Throwable) {
        null
    }

    fun saveAccountId(id: String) {
        prefs.edit().putString(KEY_ACCOUNT, id).apply()
    }

    fun getAccountId(): String? = try {
        prefs.getString(KEY_ACCOUNT, null)
    } catch (e: Throwable) {
        null
    }

    fun saveAccountName(name: String) {
        prefs.edit().putString(KEY_ACCOUNT_NAME, name).apply()
    }

    fun getAccountName(): String? = try {
        prefs.getString(KEY_ACCOUNT_NAME, null)
    } catch (e: Throwable) {
        null
    }

    fun clear() {
        try {
            prefs.edit().clear().apply()
        } catch (e: Throwable) {
            Log.w(TAG, "clear fallo: ${e.message}")
        }
    }
}
