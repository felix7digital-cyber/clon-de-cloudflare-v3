package com.cfadmin.pro.ui.screens.pages

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cfadmin.pro.ui.components.Badge
import com.cfadmin.pro.ui.components.CfCard
import com.cfadmin.pro.ui.components.Chip
import com.cfadmin.pro.ui.theme.CfCyan
import com.cfadmin.pro.ui.theme.CfEmerald
import com.cfadmin.pro.ui.theme.CfOrange
import com.cfadmin.pro.ui.theme.CfPurple
import com.cfadmin.pro.ui.theme.CfRed
import com.cfadmin.pro.ui.theme.CfYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagesDetailScreen(projectName: String, onBack: () -> Unit, vm: PagesViewModel = viewModel()) {
    val state by vm.detailState.collectAsState()
    var showBindD1 by remember { mutableStateOf(false) }
    var showBindR2 by remember { mutableStateOf(false) }
    var showBindKv by remember { mutableStateOf(false) }

    LaunchedEffect(projectName) { vm.loadDetail(projectName) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(projectName, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
                actions = { IconButton(onClick = { vm.loadDetail(projectName) }) { Icon(Icons.Default.Refresh, contentDescription = "Refrescar") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = CfOrange) }
                state.error != null && state.project == null -> Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) { Text(state.error ?: "", color = CfRed) }
                state.project != null -> {
                    val project = state.project!!
                    val prod = project.deploymentConfigs.production
                    val boundD1Ids = prod.d1Databases.values.map { it.id }.toSet()
                    val boundR2Names = prod.r2Buckets.values.map { it.name }.toSet()
                    val boundKvIds = prod.kvNamespaces.values.map { it.id }.toSet()
                    val availableD1 = state.d1List.filter { it.uuid !in boundD1Ids }
                    val availableR2 = state.r2List.filter { it.name !in boundR2Names }
                    val availableKv = state.kvList.filter { it.id !in boundKvIds }

                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        state.infoMessage?.let { msg ->
                            Surface(color = CfEmerald.copy(alpha = 0.1f), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                Text(msg, color = CfEmerald, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                            }
                        }
                        state.error?.let { msg ->
                            Surface(color = CfRed.copy(alpha = 0.1f), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                Text(msg, color = CfRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                            }
                        }

                        CfCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Badge("Pages", CfOrange)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(project.name, style = MaterialTheme.typography.titleMedium)
                                    Text(project.subdomain, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Chip("id: " + project.id.take(8))
                                Chip("rama: " + project.productionBranch)
                            }
                            if (project.createdOn.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text("Creado: " + project.createdOn, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        BindingSection("D1 Databases", prod.d1Databases.size.toString() + " binding(s)", CfCyan, state.busy, { showBindD1 = true }, prod.d1Databases.map { (n, b) -> n to b.id })
                        BindingSection("R2 Buckets", prod.r2Buckets.size.toString() + " binding(s)", CfPurple, state.busy, { showBindR2 = true }, prod.r2Buckets.map { (n, b) -> n to b.name })
                        BindingSection("KV Namespaces", prod.kvNamespaces.size.toString() + " binding(s)", CfYellow, state.busy, { showBindKv = true }, prod.kvNamespaces.map { (n, b) -> n to b.id })
                    }

                    if (showBindD1) BindOrCreateDialog(
                        title = "Vincular D1 Database",
                        existingHeader = "Elige una base de datos existente:",
                        existingEmpty = "No hay bases D1 disponibles para vincular.",
                        existingItems = availableD1.map { it.name to it.uuid },
                        createLabel = "Nombre de la nueva base de datos",
                        bindingLabel = "Nombre del binding (ej. DB)",
                        defaultBinding = "DB",
                        onDismiss = { showBindD1 = false },
                        onBindExisting = { id, bindingName, done -> vm.bindExistingD1(projectName, id, bindingName, done) },
                        onCreateNew = { name, bindingName, done -> vm.createAndBindD1(projectName, name, bindingName, done) }
                    )
                    if (showBindR2) BindOrCreateDialog(
                        title = "Vincular R2 Bucket",
                        existingHeader = "Elige un bucket existente:",
                        existingEmpty = "No hay buckets R2 disponibles para vincular.",
                        existingItems = availableR2.map { it.name to it.name },
                        createLabel = "Nombre del nuevo bucket",
                        bindingLabel = "Nombre del binding (ej. BUCKET)",
                        defaultBinding = "BUCKET",
                        onDismiss = { showBindR2 = false },
                        onBindExisting = { name, bindingName, done -> vm.bindExistingR2(projectName, name, bindingName, done) },
                        onCreateNew = { name, bindingName, done -> vm.createAndBindR2(projectName, name, bindingName, done) }
                    )
                    if (showBindKv) BindOrCreateDialog(
                        title = "Vincular KV Namespace",
                        existingHeader = "Elige un namespace existente:",
                        existingEmpty = "No hay namespaces KV disponibles para vincular.",
                        existingItems = availableKv.map { it.title to it.id },
                        createLabel = "Titulo del nuevo namespace",
                        bindingLabel = "Nombre del binding (ej. KV)",
                        defaultBinding = "KV",
                        onDismiss = { showBindKv = false },
                        onBindExisting = { id, bindingName, done -> vm.bindExistingKv(projectName, id, bindingName, done) },
                        onCreateNew = { title, bindingName, done -> vm.createAndBindKv(projectName, title, bindingName, done) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BindingSection(title: String, subtitle: String, accent: Color, busy: Boolean, onAdd: () -> Unit, entries: List<Pair<String, String>>) {
    CfCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onAdd, enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Anadir")
            }
        }
        if (entries.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Sin bindings configurados", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Spacer(Modifier.height(10.dp))
            entries.forEach { (name, value) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.bodyMedium, color = accent, fontFamily = FontFamily.Monospace, modifier = Modifier.width(120.dp))
                    Text(value, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun BindOrCreateDialog(
    title: String, existingHeader: String, existingEmpty: String,
    existingItems: List<Pair<String, String>>,
    createLabel: String, bindingLabel: String, defaultBinding: String,
    onDismiss: () -> Unit,
    onBindExisting: (id: String, bindingName: String, done: (Boolean) -> Unit) -> Unit,
    onCreateNew: (name: String, bindingName: String, done: (Boolean) -> Unit) -> Unit
) {
    val hasExisting = existingItems.isNotEmpty()
    var mode by remember { mutableStateOf(if (hasExisting) "existing" else "new") }
    var selectedId by remember { mutableStateOf(existingItems.firstOrNull()?.second.orEmpty()) }
    var newName by remember { mutableStateOf("") }
    var binding by remember { mutableStateOf(defaultBinding) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = mode == "existing", onClick = { mode = "existing" }, enabled = hasExisting && !submitting, label = { Text("Vincular existente") })
                    FilterChip(selected = mode == "new", onClick = { mode = "new" }, enabled = !submitting, label = { Text("Crear nuevo") })
                }
                if (mode == "existing") {
                    if (!hasExisting) {
                        Text(existingEmpty, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text(existingHeader, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(Modifier.fillMaxWidth().heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                            existingItems.forEach { (display, id) ->
                                Row(
                                    Modifier.fillMaxWidth().clickable(enabled = !submitting) { selectedId = id }.padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = selectedId == id, onClick = { selectedId = id }, enabled = !submitting)
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(display, style = MaterialTheme.typography.bodyMedium)
                                        if (display != id) Text(id, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text(createLabel) }, singleLine = true, enabled = !submitting, modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(value = binding, onValueChange = { binding = it }, label = { Text(bindingLabel) }, singleLine = true, enabled = !submitting, modifier = Modifier.fillMaxWidth())
                if (error != null) Text(error!!, color = CfRed, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            val valid = !submitting && binding.isNotBlank() && when (mode) { "existing" -> selectedId.isNotBlank(); else -> newName.isNotBlank() }
            TextButton(
                enabled = valid,
                onClick = {
                    submitting = true; error = null
                    if (mode == "existing") {
                        onBindExisting(selectedId, binding.trim()) { ok -> submitting = false; if (ok) onDismiss() else error = "No se pudo vincular" }
                    } else {
                        onCreateNew(newName.trim(), binding.trim()) { ok -> submitting = false; if (ok) onDismiss() else error = "No se pudo crear ni vincular" }
                    }
                }
            ) {
                if (submitting) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp), color = CfOrange)
                    Spacer(Modifier.width(8.dp))
                    Text("Procesando...", color = CfOrange)
                } else Text(if (mode == "existing") "Vincular" else "Crear y vincular", color = CfOrange)
            }
        },
        dismissButton = { TextButton(enabled = !submitting, onClick = onDismiss) { Text("Cancelar") } }
    )
}
