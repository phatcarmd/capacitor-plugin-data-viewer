package com.phatvuong.plugin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

class DataExplorerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dbFiles = DatabaseRepository.getInstance().getDatabaseFiles(this)
        val prefFiles = DatabaseRepository.getInstance().getSharedPreferencesFiles(this)

        setContent { DatabasesListScreen(dbFiles, prefFiles) }
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title)
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
        if (index >= 0) {
            selectedFiles.removeAt(index)
        } else {
            selectedFiles.add(file)
        }
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
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            item {
                SectionHeader(title = "Databases", icon = Icons.Default.Storage)
            }
            items(fileNames) {
                fileName ->
                val item = ExplorerFile(
                    name = fileName,
                    absolutePath = context.getDatabasePath(fileName).absolutePath,
                    category = ExplorerCategory.Database
                )
                val isSelected = selectedFiles.any { it.absolutePath == item.absolutePath }

                ListItem(
                    headlineContent = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(fileName, modifier = Modifier.padding(8.dp))
                            if (isSelectionMode) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Circle,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    },
                    modifier = Modifier.clickable {
                        if (isSelectionMode) {
                            toggleSelection(item)
                        } else {
                            val intent = Intent(context, TablesActivity::class.java).apply {
                                putExtra("DB_NAME", fileName)
                            }
                            context.startActivity(intent)
                        }
                    }
                )
                HorizontalDivider()
            }

            item {
                SectionHeader(title = "Shared Preferences", icon = Icons.Default.Settings)
            }
            items(prefFiles) { prefName ->
                val prefsFile = File(context.applicationInfo.dataDir, "shared_prefs/$prefName.xml")
                val item = ExplorerFile(
                    name = prefName,
                    absolutePath = prefsFile.absolutePath,
                    category = ExplorerCategory.Preference
                )
                val isSelected = selectedFiles.any { it.absolutePath == item.absolutePath }

                ListItem(
                    headlineContent = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(prefName, modifier = Modifier.padding(8.dp))
                            if (isSelectionMode) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Circle,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    },
                    modifier = Modifier.clickable {
                        if (isSelectionMode) {
                            toggleSelection(item)
                        } else {
                            val intent = Intent(context, SharedPreferencesActivity::class.java).apply {
                                putExtra("PREF_NAME", prefName)
                            }
                            context.startActivity(intent)
                        }
                    }
                )
                HorizontalDivider()
            }
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