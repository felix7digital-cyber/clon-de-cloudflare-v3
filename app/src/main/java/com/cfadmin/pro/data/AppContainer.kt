package com.cfadmin.pro.data

import android.content.Context
import com.cfadmin.pro.data.auth.AuthRepository
import com.cfadmin.pro.data.auth.TokenStore
import com.cfadmin.pro.data.repository.AccountRepository
import com.cfadmin.pro.data.repository.D1Repository
import com.cfadmin.pro.data.repository.KvRepository
import com.cfadmin.pro.data.repository.PagesRepository
import com.cfadmin.pro.data.repository.R2Repository

class AppContainer(context: Context) {
    val tokenStore: TokenStore = TokenStore(context)
    val authRepository: AuthRepository = AuthRepository(tokenStore)
    val accountRepository: AccountRepository = AccountRepository(authRepository)
    val pagesRepository: PagesRepository = PagesRepository(authRepository)
    val d1Repository: D1Repository = D1Repository(authRepository)
    val r2Repository: R2Repository = R2Repository(authRepository)
    val kvRepository: KvRepository = KvRepository(authRepository)
}
