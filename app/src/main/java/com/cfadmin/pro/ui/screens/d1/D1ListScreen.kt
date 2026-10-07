package com.cfadmin.pro.ui.screens.d1

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cfadmin.pro.data.api.dto.D1Database
import com.cfadmin.pro.ui.components.Badge
import com.cfadmin.pro.ui.components.CfCard
import com.cfadmin.pro.ui.components.Chip
import com.cfadmin.pro.ui.components.EmptyState
import com.cfadmin.pro.ui.components.SectionHeader
import com.cfadmin.pro.ui.components.ToastBus
import com.cfadmin.pro.ui.components.ToastType
import com.cfadmin.pro.ui.theme.CfCyan
import com.cfadmin.pro.ui.theme.CfOrange
import com.cfadmin.pro.ui.theme.CfRed

@Composable
fun D1ListScreen(
    onOpenDatabase: (String) -> Unit,
    vm: D1ViewModel = viewModel()
) {
    val state by vm.listState.collectAsState()

    LaunchedEffect(Unit) {
        if (state.databases.isEmpty() && !state.loading) vm.refreshList()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        CfCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeader(
                    "D1 Databases",
                    state.databases.size.toString() + " base(s) disponibles"
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { vm.refreshList() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                }
            }
        }

        if (state.error != null) {
            Spacer(Modifier.height(12.dp))
            Surface(
                color = CfRed.copy(alpha = 0.1f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    state.error ?: "",
                    color = CfRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        when {
            state.loading && state.databases.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CfCyan)
                }
            }
            state.databases.isEmpty() -> {
                EmptyState("No tienes bases D1. Créalas desde la pantalla de Pages o en dash.cloudflare.com.")
            }
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.databases, key = { it.uuid }) { db ->
                        D1DatabaseCard(
                            database = db,
                            onOpen = { onOpenDatabase(db.uuid) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun D1DatabaseCard(database: D1Database, onOpen: () -> Unit) {
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable { onOpen() }
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Badge("D1", CfCyan)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(database.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        database.uuid,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }
                IconButton(
                    onClick = { copyToClipboard(context, "UUID", database.uuid) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copiar UUID",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                database.numTables?.let { Chip("tablas: " + it) }
                database.fileSize?.let { Chip("size: " + humanSize(it)) }
                database.version?.takeIf { it.isNotBlank() }?.let { Chip("v: " + it.take(10)) }
                if (database.createdAt.isNotBlank()) {
                    Chip("creado: " + database.createdAt.take(10))
                }
            }
        }
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
