package com.cfadmin.pro.data.repository

import com.cfadmin.pro.data.api.dto.CreatePagesProjectRequest
import com.cfadmin.pro.data.api.dto.D1Binding
import com.cfadmin.pro.data.api.dto.DeploymentConfigs
import com.cfadmin.pro.data.api.dto.EnvironmentConfig
import com.cfadmin.pro.data.api.dto.KvBinding
import com.cfadmin.pro.data.api.dto.PagesProject
import com.cfadmin.pro.data.api.dto.R2Binding
import com.cfadmin.pro.data.api.dto.UpdatePagesProjectRequest
import com.cfadmin.pro.data.api.safeApiCall
import com.cfadmin.pro.data.api.unwrap
import com.cfadmin.pro.data.auth.AuthRepository

class PagesRepository(private val auth: AuthRepository) {

    private val api get() = auth.api

    suspend fun listProjects(accountId: String): List<PagesProject> = safeApiCall {
        api.listPagesProjects(accountId).unwrap()
    }

    suspend fun getProject(accountId: String, name: String): PagesProject = safeApiCall {
        api.getPagesProject(accountId, name).unwrap()
    }

    suspend fun createProject(
        accountId: String,
        name: String,
        productionBranch: String
    ): PagesProject = safeApiCall {
        api.createPagesProject(
            accountId,
            CreatePagesProjectRequest(name = name, productionBranch = productionBranch)
        ).unwrap()
    }

    suspend fun deleteProject(accountId: String, name: String) = safeApiCall {
        api.deletePagesProject(accountId, name).unwrap()
    }

    suspend fun bind(
        accountId: String,
        projectName: String,
        d1BindingName: String? = null,
        d1Id: String? = null,
        r2BindingName: String? = null,
        r2BucketName: String? = null,
        kvBindingName: String? = null,
        kvId: String? = null
    ): PagesProject = safeApiCall {
        val current = api.getPagesProject(accountId, projectName).unwrap()
        val prod = current.deploymentConfigs.production

        val newD1 = prod.d1Databases.toMutableMap()
        val newR2 = prod.r2Buckets.toMutableMap()
        val newKv = prod.kvNamespaces.toMutableMap()

        if (d1BindingName != null && d1Id != null) newD1[d1BindingName] = D1Binding(d1Id)
        if (r2BindingName != null && r2BucketName != null) newR2[r2BindingName] = R2Binding(r2BucketName)
        if (kvBindingName != null && kvId != null) newKv[kvBindingName] = KvBinding(kvId)

        val newEnv = EnvironmentConfig(
            d1Databases = newD1,
            r2Buckets = newR2,
            kvNamespaces = newKv,
            compatibilityDate = prod.compatibilityDate,
            compatibilityFlags = prod.compatibilityFlags
        )

        val newConfigs = DeploymentConfigs(
            production = newEnv,
            preview = current.deploymentConfigs.preview
        )

        api.updatePagesProject(
            accountId,
            projectName,
            UpdatePagesProjectRequest(newConfigs)
        ).unwrap()
    }
}
