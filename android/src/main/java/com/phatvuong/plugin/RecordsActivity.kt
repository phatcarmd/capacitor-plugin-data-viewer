package com.phatvuong.plugin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

class RecordsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dbName = intent.getStringExtra("DB_NAME") ?: ""
        val tableName = intent.getStringExtra("TABLE_NAME") ?: ""

        setContent { DataGridScreen(dbName, tableName) }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DataGridScreen(dbName: String, tableName: String) {
    val context = LocalContext.current
    val act = LocalActivity.current
    val repository = DatabaseRepository.getInstance()
    var allColumns by remember { mutableStateOf(emptyList<String>()) }
    var visibleColumns by remember { mutableStateOf(emptySet<String>()) }
    val rowData = remember { mutableStateListOf<List<String>>() }
    var currentPage by remember { mutableIntStateOf(0) }
    var canLoadMore by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }

    var selectedCellData by remember { mutableStateOf<String?>(null) }
    var showCellInfoDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var currentFilters by remember { mutableStateOf(emptyList<FilterCondition>()) }

    val visibleIndices = allColumns.indices.filter { allColumns[it] in visibleColumns }

    val horizontalScrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        isLoading = true
        val (cols, rows) = repository.getTableData(context, dbName, tableName, 0, 100, currentFilters)
        allColumns = cols
        visibleColumns = cols.toSet()
        rowData.addAll(rows)
        isLoading = false
    }

    LaunchedEffect(currentPage, currentFilters) {
        isLoading = true
        val (cols, newRows) = repository.getTableData(
            context, dbName, tableName,
            currentPage, 100,
            currentFilters
        )

        if (allColumns.isEmpty()) {
            allColumns = cols
            visibleColumns = cols.toSet()
        }

        if (currentPage == 0) {
            rowData.clear()
        }

        if (newRows.isNotEmpty()) {
            rowData.addAll(newRows)
            canLoadMore = true
        } else {
            canLoadMore = false
        }
        isLoading = false
    }

    if (showSettingsDialog) {
        TableSettingsDialog(
            allColumns = allColumns,
            visibleColumns = visibleColumns,
            currentFilters = currentFilters,
            onDismiss = { showSettingsDialog = false },
            onUpdateColumns = { newList -> visibleColumns = newList },
            onUpdateFilters = { newFilters ->
                if (currentFilters != newFilters) {
                    currentFilters = newFilters
                    currentPage = 0
                    canLoadMore = true
                }
            }
        )
    }

    if (showCellInfoDialog && selectedCellData != null) {
        CellDetailDialog(
            data = selectedCellData!!,
            onDismiss = { showCellInfoDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tableName, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { act?.finish() }) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onPrimary)
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
        Column(modifier = Modifier.padding(padding)) {
            Box(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {

                    stickyHeader {
                        if (visibleColumns.isNotEmpty()) {
                            Surface(shadowElevation = 2.dp) {
                                TableRow(allColumns.filter { it in visibleColumns }, isHeader = true)
                            }
                        }
                    }

                    itemsIndexed(rowData) { index, row ->
                        if (index >= rowData.size - 1 && canLoadMore && !isLoading) {
                            LaunchedEffect(Unit) {
                                isLoading = true
                                currentPage++
                                val (_, newRows) = repository.getTableData(context, dbName, tableName, currentPage, 100, currentFilters)
                                if (newRows.isEmpty()) {
                                    canLoadMore = false
                                } else {
                                    rowData.addAll(newRows)
                                }
                                isLoading = false
                            }
                        }

                        val filteredRow = visibleIndices.map { row.getOrNull(it) ?: "" }

                        TableRow(filteredRow, isHeader = false, onCellClick = { cellText ->
                            selectedCellData = cellText
                            showCellInfoDialog = true
                        })
                    }

                    if (isLoading && rowData.isNotEmpty()) {
                        item {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TableRow(row: List<String>, isHeader: Boolean = false, onCellClick: ((String) -> Unit)? = null) {
    val bgColor = if (isHeader) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.surface
    val fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .height(IntrinsicSize.Min)
        ) {
            row.forEachIndexed { index, cell ->
                // Nội dung ô dữ liệu
                Text(
                    text = cell,
                    modifier = Modifier
                        .width(150.dp)
                        .clickable(enabled = !isHeader) {
                            onCellClick?.invoke(cell)
                        }
                        .padding(12.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = fontWeight,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (index < row.size - 1) {
                    VerticalDivider(
                        modifier = Modifier.fillMaxHeight(),
                        thickness = 0.5.dp,
                        color = Color.LightGray
                    )
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
    }
}

@Composable
fun CellDetailDialog(data: String, onDismiss: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cell Detail") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(text = data, style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                clipboardManager.setText(AnnotatedString(data))
                // Có thể thêm Toast thông báo "Copied" tại đây
                onDismiss()
            }) {
                Text("Copy")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun TableSettingsDialog(
    allColumns: List<String>,
    visibleColumns: Set<String>,
    currentFilters: List<FilterCondition>,
    onDismiss: () -> Unit,
    onUpdateColumns: (Set<String>) -> Unit,
    onUpdateFilters: (List<FilterCondition>) -> Unit
) {
    var tempSelectedColumns by remember { mutableStateOf(visibleColumns) }
    val tempFilters = remember { mutableStateListOf<FilterCondition>().apply { addAll(currentFilters) } }

    var isColumnsExpanded by remember { mutableStateOf(true) }
    var isFiltersExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = null) // Icon tinh chỉnh
                Spacer(modifier = Modifier.width(8.dp))
                Text("Table Settings")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // --- PHẦN 1: QUẢN LÝ CỘT HIỂN THỊ ---
                SettingSectionHeader(
                    title = "Display Columns",
                    isExpanded = isColumnsExpanded,
                    onToggle = { isColumnsExpanded = !isColumnsExpanded },
                    badgeCount = tempSelectedColumns.size
                )

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedVisibility(visible = isColumnsExpanded) {
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        allColumns.forEach { column ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        tempSelectedColumns = if (tempSelectedColumns.contains(column)) {
                                            tempSelectedColumns - column
                                        } else {
                                            tempSelectedColumns + column
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = tempSelectedColumns.contains(column), onCheckedChange = null)
                                Text(text = column, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // --- PHẦN 2: BỘ LỌC DỮ LIỆU (MỚI) ---
                SettingSectionHeader(
                    title = "Data Filters",
                    isExpanded = isFiltersExpanded,
                    onToggle = { isFiltersExpanded = !isFiltersExpanded },
                    badgeCount = tempFilters.size
                )

                AnimatedVisibility(visible = isFiltersExpanded) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { tempFilters.add(FilterCondition(allColumns.firstOrNull() ?: "")) }) {
                                Text("+ Add Filter")
                            }
                        }

                        tempFilters.forEachIndexed { index, filter ->
                            FilterRow(
                                allColumns = allColumns,
                                filter = filter,
                                onFilterChange = { updated -> tempFilters[index] = updated },
                                onDelete = { tempFilters.removeAt(index) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onUpdateColumns(tempSelectedColumns)
                onUpdateFilters(tempFilters.toList())
                onDismiss()
            }) { Text("Apply Changes") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterRow(
    allColumns: List<String>,
    filter: FilterCondition,
    onFilterChange: (FilterCondition) -> Unit,
    onDelete: () -> Unit
) {
    var columnExpanded by remember { mutableStateOf(false) }
    var operatorExpanded by remember { mutableStateOf(false) }

    val operators = listOf("=", "LIKE", "IN", ">", "<", ">=", "<=", "!=")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), MaterialTheme.shapes.small)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExposedDropdownMenuBox(
                expanded = columnExpanded,
                onExpandedChange = { columnExpanded = !columnExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = filter.column,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Column") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = columnExpanded) },
                    modifier = Modifier.menuAnchor(),
                    textStyle = MaterialTheme.typography.bodySmall
                )
                ExposedDropdownMenu(
                    expanded = columnExpanded,
                    onDismissRequest = { columnExpanded = false }
                ) {
                    allColumns.forEach { col ->
                        DropdownMenuItem(
                            text = { Text(col) },
                            onClick = {
                                onFilterChange(filter.copy(column = col))
                                columnExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            ExposedDropdownMenuBox(
                expanded = operatorExpanded,
                onExpandedChange = { operatorExpanded = !operatorExpanded },
                modifier = Modifier.width(100.dp)
            ) {
                OutlinedTextField(
                    value = filter.operator,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Op") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = operatorExpanded) },
                    modifier = Modifier.menuAnchor(),
                    textStyle = MaterialTheme.typography.bodySmall
                )
                ExposedDropdownMenu(
                    expanded = operatorExpanded,
                    onDismissRequest = { operatorExpanded = false }
                ) {
                    operators.forEach { op ->
                        DropdownMenuItem(
                            text = { Text(op) },
                            onClick = {
                                onFilterChange(filter.copy(operator = op))
                                operatorExpanded = false
                            }
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = filter.value,
            onValueChange = { onFilterChange(filter.copy(value = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Value to search...") },
            placeholder = { Text(
                when(filter.operator) {
                    "LIKE" -> "e.g. pattern"
                    "IN" -> "e.g. 1, 2, 3 or 'A', 'B'"
                    else -> "e.g. 123"
                }
            ) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun SettingSectionHeader(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    badgeCount: Int = 0
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )

        if (badgeCount > 0 && !isExpanded) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = badgeCount.toString(),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline
        )
    }
}