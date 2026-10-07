package com.cfadmin.pro.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class D1Database(
    val uuid: String = "",
    val name: String = "",
    @SerialName("created_at") val createdAt: String = "",
    val version: String = "",
    @SerialName("num_tables") val numTables: Int? = null,
    @SerialName("file_size") val fileSize: Long? = null
)

@Serializable
data class CreateD1Request(val name: String)

@Serializable
data class D1QueryRequest(val sql: String, val params: List<String> = emptyList())

@Serializable
data class D1QueryResult(
    val results: List<JsonObject> = emptyList(),
    val success: Boolean = true,
    val meta: JsonObject? = null
)

@Serializable
data class R2BucketsResult(val buckets: List<R2Bucket> = emptyList())

@Serializable
data class R2Bucket(
    val name: String = "",
    @SerialName("creation_date") val creationDate: String = ""
)

@Serializable
data class CreateR2Request(val name: String)

@Serializable
data class KvNamespace(
    val id: String = "",
    val title: String = "",
    @SerialName("supports_url_encoding") val supportsUrlEncoding: Boolean = true
)

@Serializable
data class CreateKvRequest(val title: String)
