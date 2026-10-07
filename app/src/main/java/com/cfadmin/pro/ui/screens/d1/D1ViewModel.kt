package com.cfadmin.pro.ui.screens.d1

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cfadmin.pro.CfAdminApplication
import com.cfadmin.pro.data.api.dto.D1Database
import com.cfadmin.pro.ui.components.ToastBus
import com.cfadmin.pro.ui.components.ToastType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

data class D1ListState(
    val loading: Boolean = false,
    val databases: List<D1Database> = emptyList(),
    val error: String? = null
)

data class D1DetailState(
    val loading: Boolean = true,
    val database: D1Database? = null,
    val sqlInput: String = "SELECT name FROM sqlite_master WHERE type='table' LIMIT 50;",
    val executing: Boolean = false,
    val lastResult: List<JsonObject> = emptyList(),
    val lastError: String? = null,
    val rowCount: Int = 0,
    val tables: List<String> = emptyList(),
    val loadingTables: Boolean = false,
    val bindingsProjects: List<String> = emptyList(),
    val loadingBindings: Boolean = false,
    val deleting: Boolean = false,
    val error: String? = null
)

class D1ViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as CfAdminApplication).container
    private val auth = container.authRepository
    private val accountRepo = container.accountRepository
    private val d1Repo = container.d1Repository
    private val pagesRepo = container.pagesRepository

    private val _listState = MutableStateFlow(D1ListState())
    val listState: StateFlow<D1ListState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(D1DetailState())
    val detailState: StateFlow<D1DetailState> = _detailState.asStateFlow()

    fun accountIdOrNull(): String? = auth.getAccountId()

    private suspend fun resolveAccountId(): String? {
        auth.getAccountId()?.let { return it }
        return try {
            val acc = accountRepo.ensureAccount()
            acc.id
        } catch (e: Exception) {
            _listState.update { it.copy(error = e.message ?: "Error consultando cuentas") }
            null
        }
    }

    fun refreshList() {
        viewModelScope.launch {
            _listState.update { it.copy(loading = true, error = null) }
            try {
                val accountId = resolveAccountId() ?: run {
                    _listState.update { it.copy(loading = false) }
                    return@launch
                }
                val list = d1Repo.list(accountId)
                _listState.update { it.copy(loading = false, databases = list, error = null) }
            } catch (e: Exception) {
                _listState.update { it.copy(loading = false, error = e.message ?: "Error cargando D1") }
            }
        }
    }

    fun loadDetail(databaseId: String) {
        viewModelScope.launch {
            _detailState.update { it.copy(loading = true, error = null, lastError = null) }
            try {
                val accountId = resolveAccountId() ?: run {
                    _detailState.update { it.copy(loading = false) }
                    return@launch
                }
                val db = d1Repo.get(accountId, databaseId)
                _detailState.update { it.copy(loading = false, database = db, error = null) }
                loadTables(accountId, databaseId)
                loadBindings(accountId, databaseId)
            } catch (e: Exception) {
                _detailState.update { it.copy(loading = false, error = e.message ?: "Error cargando base D1") }
            }
        }
    }

    fun onSqlChange(sql: String) {
        _detailState.update { it.copy(sqlInput = sql) }
    }

    fun executeSql() {
        val db = _detailState.value.database ?: return
        val sql = _detailState.value.sqlInput.trim()
        if (sql.isEmpty()) {
            ToastBus.show("El SQL esta vacio", ToastType.Warning)
            return
        }
        viewModelScope.launch {
            _detailState.update { it.copy(executing = true, lastError = null, lastResult = emptyList()) }
            try {
                val accountId = resolveAccountId() ?: run {
                    _detailState.update { it.copy(executing = false) }
                    return@launch
                }
                val results = d1Repo.query(accountId, db.uuid, sql)
                val rows = results.firstOrNull()?.results ?: emptyList()
                _detailState.update {
                    it.copy(executing = false, lastResult = rows, rowCount = rows.size, lastError = null)
                }
                ToastBus.show(rows.size.toString() + " fila(s)", ToastType.Success)
            } catch (e: Exception) {
                val msg = e.message ?: "Error ejecutando SQL"
                _detailState.update { it.copy(executing = false, lastError = msg, lastResult = emptyList(), rowCount = 0) }
                ToastBus.show(msg, ToastType.Error)
            }
        }
    }

    private fun loadTables(accountId: String, databaseId: String) {
        viewModelScope.launch {
            _detailState.update { it.copy(loadingTables = true) }
            try {
                val results = d1Repo.query(
                    accountId, databaseId,
                    "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE '_cf_%' ORDER BY name;"
                )
                val tables = results.firstOrNull()?.results.orEmpty().mapNotNull { obj ->
                    obj["name"]?.toString()?.trim('"')
                }
                _detailState.update { it.copy(loadingTables = false, tables = tables) }
            } catch (e: Exception) {
                _detailState.update { it.copy(loadingTables = false) }
            }
        }
    }

    fun previewTable(tableName: String) {
        val db = _detailState.value.database ?: return
        val safe = tableName.replace("\"", "\"\"")
        val sql = "SELECT * FROM \"" + safe + "\" LIMIT 100;"
        _detailState.update { it.copy(sqlInput = sql) }
        executeSql()
    }

    private fun loadBindings(accountId: String, databaseId: String) {
        viewModelScope.launch {
            _detailState.update { it.copy(loadingBindings = true) }
            try {
                val projects = pagesRepo.listProjects(accountId)
                val bound = projects.filter { p ->
                    p.deploymentConfigs.production.d1Databases.values.any { it.id == databaseId } ||
                    p.deploymentConfigs.preview.d1Databases.values.any { it.id == databaseId }
                }.map { it.name }
                _detailState.update { it.copy(loadingBindings = false, bindingsProjects = bound) }
            } catch (e: Exception) {
                _detailState.update { it.copy(loadingBindings = false) }
            }
        }
    }

    fun deleteDatabase(onDone: (Boolean) -> Unit) {
        val db = _detailState.value.database ?: run { onDone(false); return }
        viewModelScope.launch {
            _detailState.update { it.copy(deleting = true, error = null) }
            try {
                val accountId = resolveAccountId() ?: run {
                    _detailState.update { it.copy(deleting = false) }
                    onDone(false)
                    return@launch
                }
                d1Repo.delete(accountId, db.uuid)
                ToastBus.show("Base '" + db.name + "' eliminada", ToastType.Info)
                _detailState.update { it.copy(deleting = false) }
                onDone(true)
            } catch (e: Exception) {
                val msg = e.message ?: "Error eliminando"
                _detailState.update { it.copy(deleting = false, error = msg) }
                ToastBus.show(msg, ToastType.Error)
                onDone(false)
            }
        }
    }
}
