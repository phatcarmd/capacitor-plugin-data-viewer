package com.phatvuong.plugin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

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
    val context = LocalActivity.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Explorer") },
                navigationIcon = {
                    IconButton(onClick = { context?.finish() }) {
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
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            item {
                SectionHeader(title = "Databases", icon = Icons.Default.Storage)
            }
            items(fileNames) {
                fileName -> ListItem(
                    headlineContent = { Text(fileName, modifier = Modifier.padding(8.dp)) },
                    modifier = Modifier.clickable {
                        val intent = Intent(context, TablesActivity::class.java).apply {
                            putExtra("DB_NAME", fileName)
                        }
                        context?.startActivity(intent)
                    }
                )
                HorizontalDivider()
            }

            item {
                SectionHeader(title = "Shared Preferences", icon = Icons.Default.Settings)
            }
            items(prefFiles) { prefName ->
                ListItem(
                    headlineContent = { Text(prefName, modifier = Modifier.padding(8.dp)) },
                    modifier = Modifier.clickable {
                        val intent = Intent(context, SharedPreferencesActivity::class.java).apply {
                            putExtra("PREF_NAME", prefName)
                        }
                        context?.startActivity(intent)
                    }
                )
                HorizontalDivider()
            }
        }
    }
}