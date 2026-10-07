package com.cfadmin.pro.data.api

import com.cfadmin.pro.data.api.dto.CfResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import retrofit2.HttpException

class CloudflareException(
    message: String,
    val httpCode: Int? = null
) : Exception(message)

private val errorJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

suspend fun <T> safeApiCall(block: suspend () -> T): T = try {
    block()
} catch (e: HttpException) {
    val body = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
    val msg = if (body != null) {
        try {
            val parsed = errorJson.decodeFromString<CfResponse<JsonElement>>(body)
            val cfMsg = parsed.errors.firstOrNull()?.message
            if (!cfMsg.isNullOrBlank()) "CF: $cfMsg (HTTP ${e.code()})"
            else "HTTP ${e.code()}: ${body.take(300)}"
        } catch (_: Exception) {
            "HTTP ${e.code()}: ${body.take(300)}"
        }
    } else "HTTP ${e.code()}"
    throw CloudflareException(msg, e.code())
} catch (e: CloudflareException) {
    throw e
} catch (e: Exception) {
    throw CloudflareException(e.message ?: "Error desconocido")
}

fun <T> CfResponse<T>.unwrap(): T {
    if (!success) {
        throw CloudflareException(errors.firstOrNull()?.message ?: "Cloudflare devolvio success=false")
    }
    return result ?: throw CloudflareException("Respuesta vacia de Cloudflare")
}

fun <T> CfResponse<T>.unwrapOrNull(): T? = if (success) result else null
