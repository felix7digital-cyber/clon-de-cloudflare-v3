package com.cfadmin.pro.data.repository

import com.cfadmin.pro.data.api.dto.CreateKvRequest
import com.cfadmin.pro.data.api.dto.KvNamespace
import com.cfadmin.pro.data.api.safeApiCall
import com.cfadmin.pro.data.api.unwrap
import com.cfadmin.pro.data.auth.AuthRepository

class KvRepository(private val auth: AuthRepository) {

    private val api get() = auth.api

    suspend fun list(accountId: String): List<KvNamespace> = safeApiCall {
        api.listKvNamespaces(accountId).unwrap()
    }

    suspend fun create(accountId: String, title: String): KvNamespace = safeApiCall {
        api.createKvNamespace(accountId, CreateKvRequest(title)).unwrap()
    }

    suspend fun delete(accountId: String, namespaceId: String) = safeApiCall {
        api.deleteKvNamespace(accountId, namespaceId).unwrap()
    }
}
