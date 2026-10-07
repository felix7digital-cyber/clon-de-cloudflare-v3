package com.cfadmin.pro.ui.screens.d1

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cfadmin.pro.ui.components.Badge
import com.cfadmin.pro.ui.components.CfCard
import com.cfadmin.pro.ui.components.Chip
import com.cfadmin.pro.ui.components.ToastBus
import com.cfadmin.pro.ui.components.ToastType
import com.cfadmin.pro.ui.theme.CfAmber
import com.cfadmin.pro.ui.theme.CfCyan
import com.cfadmin.pro.ui.theme.CfEmerald
import com.cfadmin.pro.ui.theme.CfOrange
import com.cfadmin.pro.ui.theme.CfPurple
import com.cfadmin.pro.ui.theme.CfRed
import com.cfadmin.pro.ui.theme.CfYellow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun D1DetailScreen(
    databaseId: String,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    vm: D1ViewModel = viewModel()
) {
    val state by vm.detailState.collectAsState()
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(databaseId) { vm.loadDetail(databaseId) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(state.database?.name ?: "D1", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.loadDetail(databaseId) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = CfRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CfCyan)
                }
                state.error != null && state.database == null -> Box(
                    Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center
                ) {
                    Text(state.error ?: "", color = CfRed)
                }
                state.database != null -> {
                    val db = state.database!!

                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // ---------- INFO ----------
                        CfCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Badge("D1", CfCyan)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(db.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        db.uuid,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                db.numTables?.let { Chip("tablas: " + it) }
                                db.fileSize?.let { Chip("size: " + humanSize(it)) }
                                if (db.createdAt.isNotBlank()) Chip("creado: " + db.createdAt.take(10))
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SmallAction("Copiar UUID") {
                                    copyToClipboard(context, "UUID", db.uuid)
                                }
                                SmallAction("Copiar link API") {
                                    val acc = vm.accountIdOrNull() ?: "<account>"
                                    val link = "https://api.cloudflare.com/client/v4/accounts/" + acc + "/d1/database/" + db.uuid
                                    copyToClipboard(context, "Link API", link)
                                }
                            }
                        }

                        // ---------- TABLAS ----------
                        CfCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Tablas", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                if (state.loadingTables) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp),
                                        color = CfCyan
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            if (state.tables.isEmpty()) {
                                Text(
                                    if (state.loadingTables) "Cargando tablas…" else "Sin tablas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 240.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    state.tables.forEach { t ->
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .clickable { vm.previewTable(t) }
                                                .padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Chip("SELECT *")
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                t,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ---------- SQL EDITOR ----------
                        CfCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("SQL", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                Chip("⌘ Tab = editor")
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = state.sqlInput,
                                onValueChange = vm::onSqlChange,
                                placeholder = { Text("SELECT * FROM mi_tabla LIMIT 50;") },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp
                                ),
                                enabled = !state.executing,
                                minLines = 4,
                                maxLines = 12,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { vm.executeSql() },
                                    enabled = !state.executing && state.sqlInput.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = CfCyan)
                                ) {
                                    if (state.executing) {
                                        CircularProgressIndicator(
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Ejecutando…")
                                    } else {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Ejecutar")
                                    }
                                }
                            }
                        }

                        // ---------- ERROR / RESULTADOS ----------
                        if (state.lastError != null) {
                            Surface(
                                color = CfRed.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    state.lastError ?: "",
                                    color = CfRed,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        if (state.lastResult.isNotEmpty()) {
                            CfCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Badge("Resultado", CfEmerald)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        state.rowCount.toString() + " fila(s)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                ResultsTable(state.lastResult)
                            }
                        }

                        // ---------- BINDINGS ----------
                        CfCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Usada en", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                if (state.loadingBindings) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp),
                                        color = CfPurple
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            if (state.loadingBindings) {
                                Text("Buscando bindings…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else if (state.bindingsProjects.isEmpty()) {
                                Text(
                                    "Ningún proyecto Pages usa esta base",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                state.bindingsProjects.forEach { name ->
                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Badge("Pages", CfOrange)
                                        Spacer(Modifier.width(8.dp))
                                        Text(name, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }

                        // ---------- ZONA DE PELIGRO ----------
                        CfCard {
                            Text("Zona de peligro", style = MaterialTheme.typography.titleMedium, color = CfRed)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Eliminar la base de datos no se puede deshacer. Si está vinculada a un proyecto Pages, primero desvincula.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = { confirmDelete = true },
                                enabled = !state.deleting,
                                colors = ButtonDefaults.buttonColors(containerColor = CfRed)
                            ) {
                                if (state.deleting) {
                                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp), color = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Eliminando…")
                                } else {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Eliminar base de datos")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Eliminar base de datos") },
            text = { Text("¿Eliminar '" + (state.database?.name ?: "") + "'? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteDatabase { ok -> if (ok) onDeleted() }
                }) { Text("Eliminar", color = CfRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") }
            }
        )
    }
}

// ----------------------------------------------------------------------
//  Componentes auxiliares
// ----------------------------------------------------------------------

@Composable
private fun SmallAction(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/**
 * Tabla de resultados. Columnas = unión de todas las keys de las filas.
 * Celdas = valor formateado (string plano sin comillas, "null" si es JsonNull).
 */
@Composable
private fun ResultsTable(rows: List<JsonObject>) {
    if (rows.isEmpty()) {
        Text("Sin filas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }

    // Unión ordenada de keys
    val columns = linkedSetOf<String>()
    rows.forEach { obj -> obj.keys.forEach { columns.add(it) } }
    val colList = columns.toList()

    val hScroll = rememberScrollState()

    Column(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(hScroll)
    ) {
        // Header
        Row(Modifier.background(CfCyan.copy(alpha = 0.12f))) {
            colList.forEach { col ->
                Text(
                    col,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    fontFamily = FontFamily.Monospace,
                    color = CfCyan,
                    modifier = Modifier
                        .width(140.dp)
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                )
            }
        }
        HorizontalDivider(color = CfCyan.copy(alpha = 0.3f))

        // Filas
        rows.forEachIndexed { i, row ->
            val bg = if (i % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            Row(Modifier.background(bg)) {
                colList.forEach { col ->
                    val cell = row[col]?.let { valueToString(it) } ?: "—"
                    Text(
                        cell,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .width(140.dp)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        }
    }
}

private fun valueToString(el: kotlinx.serialization.json.JsonElement): String {
    return when (el) {
        is JsonPrimitive -> {
            if (el.isString) el.content else el.content
        }
        else -> el.toString()
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    try {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        ToastBus.show("✓ " + label + " copiado", ToastType.Success)
    } catch (e: Exception) {
        ToastBus.show("No se pudo copiar", ToastType.Error)
    }
}

private fun humanSize(bytes: Long): String {
    if (bytes < 1024) return bytes.toString() + " B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format("%.2f MB", mb)
}
