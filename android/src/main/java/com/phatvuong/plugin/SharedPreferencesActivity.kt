package com.phatvuong.plugin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

class SharedPreferencesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fileName = intent.getStringExtra("PREF_NAME") ?: ""
        val data = DatabaseRepository.getInstance().getSharedPrefsData(this, fileName)

        setContent {
            SharedPreferencesScreen(fileName, data) { }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SharedPreferencesScreen(
    fileName: String,
    data: List<Pair<String, String>>,
    onCellClick: (String) -> Unit
) {
    val act = LocalActivity.current
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
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
                            Text(
                                text = "Key",
                                modifier = Modifier.weight(0.4f).padding(12.dp),
                            )

                            Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(dividerColor))

                            Text(
                                text = "Value",
                                modifier = Modifier.weight(0.6f).padding(12.dp),
                            )
                        }
                        HorizontalDivider(thickness = 1.dp, color = dividerColor)
                    }
                }
            }

            itemsIndexed(data) { index, item ->
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .background(
                                if (index % 2 == 0) MaterialTheme.colorScheme.surface
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            )
                            .clickable { onCellClick(item.second) },
                    ) {
                        Text(
                            text = item.first,
                            modifier = Modifier.weight(0.4f).padding(12.dp),
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(dividerColor)
                        )

                        Text(
                            text = item.second,
                            modifier = Modifier.weight(0.6f).padding(12.dp),
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                }
            }
        }
    }
}