package com.cfadmin.pro.data.repository

import com.cfadmin.pro.data.api.dto.Account
import com.cfadmin.pro.data.api.safeApiCall
import com.cfadmin.pro.data.api.unwrap
import com.cfadmin.pro.data.auth.AuthRepository

class AccountRepository(private val auth: AuthRepository) {

    private val api get() = auth.api

    suspend fun ensureAccount(): Account = safeApiCall {
        val list = api.listAccounts().unwrap()
        val first = list.firstOrNull()
            ?: throw Exception("El token no tiene cuentas asociadas")
        auth.saveAccount(first.id, first.name)
        first
    }
}
