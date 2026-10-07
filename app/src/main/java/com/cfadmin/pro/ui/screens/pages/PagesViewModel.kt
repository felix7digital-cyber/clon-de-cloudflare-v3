package com.cfadmin.pro.ui.screens.pages

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cfadmin.pro.CfAdminApplication
import com.cfadmin.pro.data.api.dto.D1Database
import com.cfadmin.pro.data.api.dto.KvNamespace
import com.cfadmin.pro.data.api.dto.PagesProject
import com.cfadmin.pro.data.api.dto.R2Bucket
import com.cfadmin.pro.ui.components.ToastBus
import com.cfadmin.pro.ui.components.ToastType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PagesListState(
    val loading: Boolean = false,
    val projects: List<PagesProject> = emptyList(),
    val error: String? = null,
    val accountId: String? = null,
    val accountName: String? = null
)

data class PagesDetailState(
    val loading: Boolean = true,
    val project: PagesProject? = null,
    val d1List: List<D1Database> = emptyList(),
    val r2List: List<R2Bucket> = emptyList(),
    val kvList: List<KvNamespace> = emptyList(),
    val error: String? = null,
    val infoMessage: String? = null,
    val busy: Boolean = false
)

class PagesViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as CfAdminApplication).container
    private val auth = container.authRepository
    private val accountRepo = container.accountRepository
    private val pagesRepo = container.pagesRepository
    private val d1Repo = container.d1Repository
    private val r2Repo = container.r2Repository
    private val kvRepo = container.kvRepository

    private val _listState = MutableStateFlow(PagesListState())
    val listState: StateFlow<PagesListState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(PagesDetailState())
    val detailState: StateFlow<PagesDetailState> = _detailState.asStateFlow()

    private suspend fun resolveAccountId(): String? {
        auth.getAccountId()?.let { return it }
        return try {
            val acc = accountRepo.ensureAccount()
            _listState.update { it.copy(accountId = acc.id, accountName = acc.name) }
            acc.id
        } catch (e: Exception) {
            _listState.update { it.copy(error = e.message ?: "Error consultando cuentas") }
            null
        }
    }

    fun refreshProjects() {
        viewModelScope.launch {
            _listState.update { it.copy(loading = true, error = null) }
            try {
                val accountId = resolveAccountId() ?: run {
                    _listState.update { it.copy(loading = false) }
                    return@launch
                }
                val projects = pagesRepo.listProjects(accountId)
                _listState.update {
                    it.copy(
                        loading = false,
                        projects = projects,
                        accountId = accountId,
                        accountName = auth.getAccountName(),
                        error = null
                    )
                }
            } catch (e: Exception) {
                _listState.update { it.copy(loading = false, error = e.message ?: "Error desconocido") }
            }
        }
    }

    fun createProject(name: String, branch: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _listState.update { it.copy(loading = true, error = null) }
            try {
                val accountId = resolveAccountId() ?: run {
                    _listState.update { it.copy(loading = false) }
                    onDone(false); return@launch
                }
                val created = pagesRepo.createProject(accountId, name.trim(), branch.trim())
                _listState.update { it.copy(loading = false, projects = listOf(created) + it.projects) }
                ToastBus.show("Proyecto '" + created.name + "' creado", ToastType.Success)
                onDone(true)
            } catch (e: Exception) {
                _listState.update { it.copy(loading = false, error = e.message ?: "Error creando proyecto") }
                onDone(false)
            }
        }
    }

    fun deleteProject(name: String) {
        viewModelScope.launch {
            try {
                val accountId = resolveAccountId() ?: return@launch
                pagesRepo.deleteProject(accountId, name)
                _listState.update { state -> state.copy(projects = state.projects.filter { it.name != name }) }
                ToastBus.show("Proyecto '" + name + "' eliminado", ToastType.Info)
            } catch (e: Exception) {
                _listState.update { it.copy(error = e.message ?: "Error eliminando") }
            }
        }
    }

    fun loadDetail(projectName: String) {
        viewModelScope.launch {
            _detailState.update { it.copy(loading = true, error = null, infoMessage = null) }
            try {
                val accountId = resolveAccountId() ?: run {
                    _detailState.update { it.copy(loading = false) }
                    return@launch
                }
                val project = pagesRepo.getProject(accountId, projectName)
                val d1 = runCatching { d1Repo.list(accountId) }.getOrDefault(emptyList())
                val r2 = runCatching { r2Repo.list(accountId) }.getOrDefault(emptyList())
                val kv = runCatching { kvRepo.list(accountId) }.getOrDefault(emptyList())
                _detailState.update {
                    it.copy(loading = false, project = project, d1List = d1, r2List = r2, kvList = kv, error = null)
                }
            } catch (e: Exception) {
                _detailState.update { it.copy(loading = false, error = e.message ?: "Error cargando proyecto") }
            }
        }
    }

    fun bindExistingD1(projectName: String, d1Id: String, bindingName: String, onDone: (Boolean) -> Unit) =
        bindGeneric(projectName, onDone, "D1 vinculada como '" + bindingName + "'") { acc ->
            pagesRepo.bind(acc, projectName, d1BindingName = bindingName, d1Id = d1Id)
        }

    fun bindExistingR2(projectName: String, bucketName: String, bindingName: String, onDone: (Boolean) -> Unit) =
        bindGeneric(projectName, onDone, "R2 vinculado como '" + bindingName + "'") { acc ->
            pagesRepo.bind(acc, projectName, r2BindingName = bindingName, r2BucketName = bucketName)
        }

    fun bindExistingKv(projectName: String, namespaceId: String, bindingName: String, onDone: (Boolean) -> Unit) =
        bindGeneric(projectName, onDone, "KV vinculado como '" + bindingName + "'") { acc ->
            pagesRepo.bind(acc, projectName, kvBindingName = bindingName, kvId = namespaceId)
        }

    private fun bindGeneric(projectName: String, onDone: (Boolean) -> Unit, successMessage: String, block: suspend (accountId: String) -> PagesProject) {
        viewModelScope.launch {
            _detailState.update { it.copy(busy = true, error = null, infoMessage = null) }
            try {
                val accountId = resolveAccountId() ?: run {
                    _detailState.update { it.copy(busy = false) }
                    onDone(false); return@launch
                }
                val updated = block(accountId)
                _detailState.update { it.copy(busy = false, project = updated, infoMessage = successMessage) }
                ToastBus.show(successMessage, ToastType.Success)
                onDone(true)
            } catch (e: Exception) {
                val msg = e.message ?: "Error vinculando"
                _detailState.update { it.copy(busy = false, error = msg) }
                ToastBus.show(msg, ToastType.Error)
                onDone(false)
            }
        }
    }

    fun createAndBindD1(projectName: String, dbName: String, bindingName: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _detailState.update { it.copy(busy = true, error = null, infoMessage = null) }
            var createdId: String? = null
            try {
                val accountId = resolveAccountId() ?: run {
                    _detailState.update { it.copy(busy = false) }
                    onDone(false); return@launch
                }
                val db = d1Repo.create(accountId, dbName)
                createdId = db.uuid
                val updated = pagesRepo.bind(accountId, projectName, d1BindingName = bindingName, d1Id = db.uuid)
                val msg = "D1 '" + dbName + "' creada y vinculada como '" + bindingName + "'"
                _detailState.update { it.copy(busy = false, project = updated, infoMessage = msg) }
                ToastBus.show(msg, ToastType.Success)
                onDone(true)
            } catch (e: Exception) {
                createdId?.let { id -> runCatching { val acc = auth.getAccountId() ?: return@runCatching; d1Repo.delete(acc, id) } }
                val msg = e.message ?: "Error creando/vinculando D1"
                _detailState.update { it.copy(busy = false, error = msg) }
                ToastBus.show(msg, ToastType.Error)
                onDone(false)
            }
        }
    }

    fun createAndBindR2(projectName: String, bucketName: String, bindingName: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _detailState.update { it.copy(busy = true, error = null, infoMessage = null) }
            var created = false
            try {
                val accountId = resolveAccountId() ?: run {
                    _detailState.update { it.copy(busy = false) }
                    onDone(false); return@launch
                }
                r2Repo.create(accountId, bucketName)
                created = true
                val updated = pagesRepo.bind(accountId, projectName, r2BindingName = bindingName, r2BucketName = bucketName)
                val msg = "R2 '" + bucketName + "' creado y vinculado como '" + bindingName + "'"
                _detailState.update { it.copy(busy = false, project = updated, infoMessage = msg) }
                ToastBus.show(msg, ToastType.Success)
                onDone(true)
            } catch (e: Exception) {
                if (created) runCatching { val acc = auth.getAccountId() ?: return@runCatching; r2Repo.delete(acc, bucketName) }
                val msg = e.message ?: "Error creando/vinculando R2"
                _detailState.update { it.copy(busy = false, error = msg) }
                ToastBus.show(msg, ToastType.Error)
                onDone(false)
            }
        }
    }

    fun createAndBindKv(projectName: String, title: String, bindingName: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _detailState.update { it.copy(busy = true, error = null, infoMessage = null) }
            var createdId: String? = null
            try {
                val accountId = resolveAccountId() ?: run {
                    _detailState.update { it.copy(busy = false) }
                    onDone(false); return@launch
                }
                val ns = kvRepo.create(accountId, title)
                createdId = ns.id
                val updated = pagesRepo.bind(accountId, projectName, kvBindingName = bindingName, kvId = ns.id)
                val msg = "KV '" + title + "' creado y vinculado como '" + bindingName + "'"
                _detailState.update { it.copy(busy = false, project = updated, infoMessage = msg) }
                ToastBus.show(msg, ToastType.Success)
                onDone(true)
            } catch (e: Exception) {
                createdId?.let { id -> runCatching { val acc = auth.getAccountId() ?: return@runCatching; kvRepo.delete(acc, id) } }
                val msg = e.message ?: "Error creando/vinculando KV"
                _detailState.update { it.copy(busy = false, error = msg) }
                ToastBus.show(msg, ToastType.Error)
                onDone(false)
            }
        }
    }
}
