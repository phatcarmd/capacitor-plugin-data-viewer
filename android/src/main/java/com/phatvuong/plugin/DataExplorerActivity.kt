package com.phatvuong.plugin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File

private val DividerIndent = 56.dp

class DataExplorerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dbFiles = DatabaseRepository.getInstance().getDatabaseFiles(this)
        val prefFiles = DatabaseRepository.getInstance().getSharedPreferencesFiles(this)

        setContent { DatabasesListScreen(dbFiles, prefFiles) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabasesListScreen(fileNames: List<String>, prefFiles: List<String>) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val selectedFiles = remember { mutableStateListOf<ExplorerFile>() }
    var isSelectionMode by remember { mutableStateOf(false) }

    fun toggleSelection(file: ExplorerFile) {
        val index = selectedFiles.indexOfFirst { it.absolutePath == file.absolutePath }
        if (index >= 0) selectedFiles.removeAt(index) else selectedFiles.add(file)
    }

    fun shareSelectedFiles() {
        if (selectedFiles.isEmpty()) return
        val uris = selectedFiles.mapNotNull { selected ->
            val file = File(selected.absolutePath)
            if (!file.exists()) return@mapNotNull null
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.dataviewer.fileprovider",
                file
            )
        }
        if (uris.isEmpty()) return
        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            putExtra(Intent.EXTRA_SUBJECT, "Data Viewer Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share files"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Explorer") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSelectionMode) {
                            isSelectionMode = false
                            selectedFiles.clear()
                        } else {
                            activity?.finish()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = if (isSelectionMode) "Cancel Selection" else "Back"
                        )
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        TextButton(
                            onClick = { shareSelectedFiles() },
                            enabled = selectedFiles.isNotEmpty()
                        ) {
                            Text("Share", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    } else {
                        TextButton(onClick = { isSelectionMode = true }) {
                            Text("Select", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        val isDark = isSystemInDarkTheme()
        val groupedBackground = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7)
        val cardBackground = if (isDark) Color(0xFF1C1C1E) else Color.White
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(groupedBackground)
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // --- DATABASES ---
            item {
                GroupSectionLabel("Databases")
            }
            item {
                GroupCard(cardBackground) {
                    fileNames.forEachIndexed { index, fileName ->
                        val item = ExplorerFile(
                            name = fileName,
                            absolutePath = context.getDatabasePath(fileName).absolutePath,
                            category = ExplorerCategory.Database
                        )
                        val isSelected = selectedFiles.any { it.absolutePath == item.absolutePath }
                        ExplorerRow(
                            icon = Icons.Default.Storage,
                            iconTint = Color(0xFF2196F3),
                            label = fileName,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onClick = {
                                if (isSelectionMode) {
                                    toggleSelection(item)
                                } else {
                                    context.startActivity(
                                        Intent(context, TablesActivity::class.java).apply {
                                            putExtra("DB_NAME", fileName)
                                        }
                                    )
                                }
                            }
                        )
                        if (index < fileNames.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = DividerIndent),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }

            // --- NETWORK ---
            item {
                GroupSectionLabel("Network")
            }
            item {
                GroupCard(cardBackground) {
                    ExplorerRow(
                        icon = Icons.Default.Language,
                        iconTint = Color(0xFF9C27B0),
                        label = "Network Calls",
                        isSelectionMode = false,
                        isSelected = false,
                        onClick = {
                            if (!isSelectionMode) {
                                context.startActivity(Intent(context, NetworkCallsActivity::class.java))
                            }
                        }
                    )
                }
            }

            // --- SHARED PREFERENCES ---
            item {
                GroupSectionLabel("Shared Preferences")
            }
            item {
                GroupCard(cardBackground) {
                    prefFiles.forEachIndexed { index, prefName ->
                        val prefsFile = File(context.applicationInfo.dataDir, "shared_prefs/$prefName.xml")
                        val item = ExplorerFile(
                            name = prefName,
                            absolutePath = prefsFile.absolutePath,
                            category = ExplorerCategory.Preference
                        )
                        val isSelected = selectedFiles.any { it.absolutePath == item.absolutePath }
                        ExplorerRow(
                            icon = Icons.Default.Settings,
                            iconTint = Color(0xFFFF9800),
                            label = prefName,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onClick = {
                                if (isSelectionMode) {
                                    toggleSelection(item)
                                } else {
                                    context.startActivity(
                                        Intent(context, SharedPreferencesActivity::class.java).apply {
                                            putExtra("PREF_NAME", prefName)
                                        }
                                    )
                                }
                            }
                        )
                        if (index < prefFiles.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = DividerIndent),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupSectionLabel(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp)
    )
}

@Composable
private fun GroupCard(containerColor: Color, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@Composable
private fun ExplorerRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (isSelectionMode) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Circle,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private enum class ExplorerCategory {
    Database,
    Preference,
}

private data class ExplorerFile(
    val name: String,
    val absolutePath: String,
    val category: ExplorerCategory,
)
