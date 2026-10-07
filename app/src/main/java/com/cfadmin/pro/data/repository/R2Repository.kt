package com.cfadmin.pro.data.repository

import com.cfadmin.pro.data.api.dto.CreateR2Request
import com.cfadmin.pro.data.api.dto.R2Bucket
import com.cfadmin.pro.data.api.safeApiCall
import com.cfadmin.pro.data.api.unwrap
import com.cfadmin.pro.data.auth.AuthRepository

class R2Repository(private val auth: AuthRepository) {

    private val api get() = auth.api

    suspend fun list(accountId: String): List<R2Bucket> = safeApiCall {
        api.listR2Buckets(accountId).unwrap().buckets
    }

    suspend fun create(accountId: String, name: String) = safeApiCall {
        api.createR2Bucket(accountId, CreateR2Request(name)).unwrap()
    }

    suspend fun delete(accountId: String, bucketName: String) = safeApiCall {
        api.deleteR2Bucket(accountId, bucketName).unwrap()
    }
}
