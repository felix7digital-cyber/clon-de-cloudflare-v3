package com.cfadmin.pro.data.repository

import com.cfadmin.pro.data.api.dto.CreateD1Request
import com.cfadmin.pro.data.api.dto.D1Database
import com.cfadmin.pro.data.api.dto.D1QueryRequest
import com.cfadmin.pro.data.api.dto.D1QueryResult
import com.cfadmin.pro.data.api.safeApiCall
import com.cfadmin.pro.data.api.unwrap
import com.cfadmin.pro.data.auth.AuthRepository

class D1Repository(private val auth: AuthRepository) {

    private val api get() = auth.api

    suspend fun list(accountId: String): List<D1Database> = safeApiCall {
        api.listD1Databases(accountId).unwrap()
    }

    suspend fun create(accountId: String, name: String): D1Database = safeApiCall {
        api.createD1Database(accountId, CreateD1Request(name)).unwrap()
    }

    suspend fun get(accountId: String, databaseId: String): D1Database = safeApiCall {
        api.getD1Database(accountId, databaseId).unwrap()
    }

    suspend fun delete(accountId: String, databaseId: String) = safeApiCall {
        api.deleteD1Database(accountId, databaseId).unwrap()
    }

    suspend fun query(
        accountId: String,
        databaseId: String,
        sql: String,
        params: List<String> = emptyList()
    ): List<D1QueryResult> = safeApiCall {
        api.queryD1Database(accountId, databaseId, D1QueryRequest(sql, params)).unwrap()
    }
}
