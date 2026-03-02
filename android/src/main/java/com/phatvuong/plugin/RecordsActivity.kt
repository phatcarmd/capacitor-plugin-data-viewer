package com.phatvuong.plugin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataGridScreen(dbName: String, tableName: String) {
    val context = LocalContext.current
    val act = LocalActivity.current
    val repository = DatabaseRepository.getInstance()
    var columns by remember { mutableStateOf(emptyList<String>()) }
    val rowData = remember { mutableStateListOf<List<String>>() }
    var currentPage by remember { mutableIntStateOf(0) }
    var canLoadMore by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }

    var selectedCellData by remember { mutableStateOf<String?>(null) }
    var showCellInfoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        val (cols, rows) = repository.getTableData(context, dbName, tableName, 0, 100)
        columns = cols
        rowData.addAll(rows)
        isLoading = false
    }

    val horizontalScrollState = rememberScrollState()

    if (showCellInfoDialog && selectedCellData != null) {
        CellDetailDialog(
            data = selectedCellData!!,
            onDismiss = { showCellInfoDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tableName) },
                navigationIcon = {
                    IconButton(onClick = { act?.finish() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back"
                        )
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
                        if (columns.isNotEmpty()) {
                            // Thêm Surface hoặc Background để header không bị trong suốt khi chồng lên row
                            Surface(shadowElevation = 2.dp) {
                                TableRow(columns, isHeader = true)
                            }
                        }
                    }

                    itemsIndexed(rowData) { index, row ->
                        if (index >= rowData.size - 1 && canLoadMore && !isLoading) {
                            LaunchedEffect(Unit) {
                                isLoading = true
                                currentPage++
                                val (_, newRows) = repository.getTableData(context, dbName, tableName, currentPage, 100)
                                if (newRows.isEmpty()) {
                                    canLoadMore = false
                                } else {
                                    rowData.addAll(newRows)
                                }
                                isLoading = false
                            }
                        }

                        TableRow(row, isHeader = false, onCellClick = { cellText ->
                            selectedCellData = cellText
                            showCellInfoDialog = true
                        })
                    }

                    // Hiển thị loading ở cuối danh sách nếu đang tải thêm
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