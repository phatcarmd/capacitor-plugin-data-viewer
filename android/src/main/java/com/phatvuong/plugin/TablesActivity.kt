package com.phatvuong.plugin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class TablesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dbName = intent.getStringExtra("DB_NAME") ?: ""

        val tables = DatabaseRepository.getInstance().getTables(this, dbName)

        setContent { ListScreen(dbName, tables) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(dbName: String, tables: List<String>) {
    val context = LocalActivity.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(dbName) },
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
            items(tables) {
                    table -> ListItem(
                headlineContent = { Text(table, modifier = Modifier.padding(8.dp)) },
                modifier = Modifier.clickable {
                    val intent = Intent(context, RecordsActivity::class.java).apply {
                        putExtra("DB_NAME", dbName)
                        putExtra("TABLE_NAME", table)
                    }
                    context?.startActivity(intent)
                }
            )
                HorizontalDivider()
            }
        }
    }
}