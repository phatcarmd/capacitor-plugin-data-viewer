package com.phatvuong.plugin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.json.JSONArray
import org.json.JSONObject

class SharedPreferencesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fileName = intent.getStringExtra("PREF_NAME") ?: ""

        setContent {
            SharedPreferencesScreen(fileName)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SharedPreferencesScreen(
    fileName: String,
) {
    val act = LocalActivity.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val repository = remember { DatabaseRepository.getInstance() }
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    val rows = remember { mutableStateListOf<SharedPrefEntry>() }

    var selectedEntry by remember { mutableStateOf<SharedPrefEntry?>(null) }
    var selectedCellData by remember { mutableStateOf("") }
    var showCellActionDialog by remember { mutableStateOf(false) }

    var showEditorDialog by remember { mutableStateOf(false) }
    var editorMode by remember { mutableStateOf(EditorMode.CREATE) }
    var editingOldKey by remember { mutableStateOf<String?>(null) }
    var draftKey by remember { mutableStateOf("") }
    var draftValue by remember { mutableStateOf("") }
    var draftType by remember { mutableStateOf("String") }
    var showTypeMenu by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var keySortOrder by remember { mutableStateOf(KeySortOrder.NONE) }
    val editableTypes = listOf("String", "Int", "Long", "Float", "Bool", "StringSet", "Data", "Dictionary", "Array")

    val displayedRows = when (keySortOrder) {
        KeySortOrder.NONE -> rows.toList()
        KeySortOrder.ASC -> rows.sortedBy { it.key.lowercase() }
        KeySortOrder.DESC -> rows.sortedByDescending { it.key.lowercase() }
    }

    fun reload() {
        rows.clear()
        rows.addAll(repository.getSharedPrefsEntries(context, fileName))
    }

    fun beginCreate() {
        editorMode = EditorMode.CREATE
        editingOldKey = null
        draftKey = ""
        draftValue = ""
        draftType = "String"
        showEditorDialog = true
    }

    fun beginEdit(entry: SharedPrefEntry) {
        editorMode = EditorMode.EDIT
        editingOldKey = entry.key
        draftKey = entry.key
        draftValue = entry.value
        draftType = editableTypes.firstOrNull { it == entry.type } ?: "String"
        showEditorDialog = true
    }

    fun validateDraft(): String? {
        val trimmedKey = draftKey.trim()
        if (trimmedKey.isEmpty()) return "Key is required."

        val duplicate = rows.any {
            it.key == trimmedKey && (editorMode == EditorMode.CREATE || it.key != editingOldKey)
        }
        if (duplicate) return "Key '$trimmedKey' already exists."

        val text = draftValue.trim()
        val invalidType = when (draftType) {
            "String" -> false
            "Int" -> text.toIntOrNull() == null
            "Long" -> text.toLongOrNull() == null
            "Float" -> text.toFloatOrNull() == null
            "Bool" -> text.lowercase() !in setOf("true", "false", "1", "0")
            "StringSet" -> text.split(",").map { it.trim() }.none { it.isNotEmpty() }
            "Data" -> {
                try {
                    android.util.Base64.decode(text, android.util.Base64.DEFAULT)
                    false
                } catch (_: Exception) {
                    true
                }
            }
            "Dictionary" -> {
                try {
                    JSONObject(text)
                    false
                } catch (_: Exception) {
                    true
                }
            }
            "Array" -> {
                try {
                    JSONArray(text)
                    false
                } catch (_: Exception) {
                    true
                }
            }
            else -> true
        }

        if (invalidType) {
            return when (draftType) {
                "Int" -> "Value must be a valid integer (e.g. 10, -4)."
                "Long" -> "Value must be a valid long integer."
                "Float" -> "Value must be a valid decimal number (Float)."
                "Bool" -> "Bool accepts: true, false, 1, 0."
                "StringSet" -> "StringSet must contain at least one comma-separated item."
                "Data" -> "Data must be a valid Base64 string."
                "Dictionary" -> "Dictionary must be a valid JSON object, e.g. {\"a\":1}."
                "Array" -> "Array must be a valid JSON array, e.g. [1, \"a\", true]."
                else -> "Invalid value."
            }
        }

        return null
    }

    val draftValidationError = validateDraft()
    val isDraftValid = draftValidationError == null

    LaunchedEffect(fileName) {
        reload()
    }

    if (showCellActionDialog && selectedEntry != null) {
        PreferenceCellActionDialog(
            data = selectedCellData,
            onCopy = {
                clipboardManager.setText(AnnotatedString(selectedCellData))
                showCellActionDialog = false
            },
            onEdit = {
                beginEdit(selectedEntry!!)
                showCellActionDialog = false
            },
            onDelete = {
                val key = selectedEntry!!.key
                val ok = repository.deleteSharedPrefEntry(context, fileName, key)
                if (ok) {
                    reload()
                } else {
                    errorMessage = "Failed to delete key '$key'."
                }
                showCellActionDialog = false
            },
            onDismiss = { showCellActionDialog = false }
        )
    }

    if (showEditorDialog) {
        PreferenceEditorDialog(
            mode = editorMode,
            draftKey = draftKey,
            draftValue = draftValue,
            draftType = draftType,
            editableTypes = editableTypes,
            showTypeMenu = showTypeMenu,
            validationError = draftValidationError,
            isDraftValid = isDraftValid,
            onKeyChange = { draftKey = it },
            onValueChange = { draftValue = it },
            onTypeChange = {
                draftType = it
                showTypeMenu = false
            },
            onTypeMenuExpandedChange = { expanded -> showTypeMenu = expanded },
            onDismiss = { showEditorDialog = false },
            onSave = {
                val saveError = repository.upsertSharedPrefEntry(
                    context = context,
                    fileName = fileName,
                    key = draftKey,
                    type = draftType,
                    rawInput = draftValue,
                    oldKey = editingOldKey
                )

                if (saveError == null) {
                    showEditorDialog = false
                    reload()
                } else {
                    errorMessage = saveError
                }
            }
        )
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Cannot Save") },
            text = { Text(errorMessage!!) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(fileName, overflow = TextOverflow.Ellipsis, maxLines = 2) },
                navigationIcon = {
                    IconButton(onClick = { act?.finish() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { beginCreate() }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding)
        ) {
            stickyHeader {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shadowElevation = 1.dp
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(0.3f)
                                    .clickable {
                                        keySortOrder = when (keySortOrder) {
                                            KeySortOrder.NONE -> KeySortOrder.ASC
                                            KeySortOrder.ASC -> KeySortOrder.DESC
                                            KeySortOrder.DESC -> KeySortOrder.NONE
                                        }
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Key")

                                if (keySortOrder != KeySortOrder.NONE) {
                                    Icon(
                                        imageVector = if (keySortOrder == KeySortOrder.ASC) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(dividerColor))

                            Text(
                                text = "Value",
                                modifier = Modifier.weight(0.5f).padding(12.dp),
                            )

                            Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(dividerColor))

                            Text(
                                text = "Type",
                                modifier = Modifier.weight(0.2f).padding(12.dp),
                            )
                        }
                        HorizontalDivider(thickness = 1.dp, color = dividerColor)
                    }
                }
            }

            itemsIndexed(displayedRows) { index, item ->
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .background(
                                if (index % 2 == 0) MaterialTheme.colorScheme.surface
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            )
                    ) {
                        Text(
                            text = item.key,
                            modifier = Modifier
                                .clickable {
                                    selectedEntry = item
                                    selectedCellData = item.key
                                    showCellActionDialog = true
                                }
                                .weight(0.3f)
                                .padding(12.dp),
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(dividerColor)
                        )

                        Text(
                            text = item.value,
                            modifier = Modifier
                                .clickable {
                                    selectedEntry = item
                                    selectedCellData = item.value
                                    showCellActionDialog = true
                                }
                                .weight(0.5f)
                                .padding(12.dp),
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(dividerColor)
                        )

                        Text(
                            text = item.type,
                            modifier = Modifier
                                .clickable {
                                    selectedEntry = item
                                    selectedCellData = item.type
                                    showCellActionDialog = true
                                }
                                .weight(0.2f)
                                .padding(12.dp),
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                }
            }
        }
    }
}

@Composable
private fun PreferenceCellActionDialog(
    data: String,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Cell Options", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = data, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.heightIn(max = 160.dp))
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onCopy) { Text("Copy") }
                    TextButton(onClick = onEdit) { Text("Edit") }
                    TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreferenceEditorDialog(
    mode: EditorMode,
    draftKey: String,
    draftValue: String,
    draftType: String,
    editableTypes: List<String>,
    showTypeMenu: Boolean,
    validationError: String?,
    isDraftValid: Boolean,
    onKeyChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    onTypeMenuExpandedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    if (mode == EditorMode.CREATE) "Add Entry" else "Edit Entry",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = draftKey,
                    onValueChange = onKeyChange,
                    label = { Text("Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = draftValue,
                    onValueChange = onValueChange,
                    label = { Text("Value") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = showTypeMenu,
                    onExpandedChange = onTypeMenuExpandedChange
                ) {
                    OutlinedTextField(
                        value = draftType,
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        label = { Text("Type") },
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTypeMenu)
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = showTypeMenu,
                        onDismissRequest = { onTypeMenuExpandedChange(false) }
                    ) {
                        editableTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    onTypeChange(type)
                                    onTypeMenuExpandedChange(false)
                                }
                            )
                        }
                    }
                }

                if (validationError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = validationError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.weight(1f))
                    if (isDraftValid) {
                        Button(onClick = onSave) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

private enum class EditorMode {
    CREATE,
    EDIT
}

private enum class KeySortOrder {
    NONE,
    ASC,
    DESC
}