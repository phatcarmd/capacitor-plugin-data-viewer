package com.phatvuong.plugin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NetworkCallsActivity : ComponentActivity() {

    private val calls = mutableStateListOf<NetworkCall>()
    private var isLoading = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NetworkCallsScreen(
                calls = calls,
                isLoading = isLoading.value,
                onRefresh = { loadFromJS() },
                onClear = { clearCalls() },
                onShare = { shareCallsAsFile() },
                onCallClick = { call ->
                    NetworkCallStore.add(call) // ensure detail can access it
                    startActivity(
                        Intent(this, NetworkCallDetailActivity::class.java).apply {
                            putExtra("CALL_ID", call.id)
                        }
                    )
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        loadFromJS()
    }

    private fun loadFromJS() {
        isLoading.value = true
        val webView = DataViewer.bridgeRef?.get()?.webView
        if (webView == null) {
            isLoading.value = false
            return
        }
        webView.post {
            // Return the array directly — evaluateJavascript serializes it as JSON for us.
            // Avoids double-encoding that JSON.stringify in JS would cause.
            webView.evaluateJavascript("window.__dvNetworkCalls || []") { json ->
                runOnUiThread {
                    try {
                        val parsed = parseCallsJson(json)
                        calls.clear()
                        calls.addAll(parsed)
                    } catch (_: Exception) {}
                    isLoading.value = false
                }
            }
        }
    }

    private fun clearCalls() {
        val webView = DataViewer.bridgeRef?.get()?.webView ?: return
        webView.post {
            webView.evaluateJavascript("window.__dvNetworkCalls = []; void 0", null)
            runOnUiThread { calls.clear() }
        }
    }

    private fun parseCallsJson(raw: String): List<NetworkCall> {
        val array = JSONArray(raw ?: "[]")
        val result = mutableListOf<NetworkCall>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                NetworkCall(
                    id = obj.optString("id"),
                    url = obj.optString("url"),
                    method = obj.optString("method", "GET"),
                    requestHeaders = obj.optJSONObject("requestHeaders").toMap(),
                    requestBody = obj.optString("requestBody").takeIf { it.isNotEmpty() && it != "null" },
                    status = obj.optInt("status", 0).takeIf { it != 0 },
                    responseHeaders = obj.optJSONObject("responseHeaders").toMap(),
                    responseBody = obj.optString("responseBody").takeIf { it.isNotEmpty() && it != "null" },
                    duration = obj.optLong("duration"),
                    timestamp = obj.optLong("timestamp"),
                    error = obj.optString("error").takeIf { it.isNotEmpty() && it != "null" }
                )
            )
        }
        return result
    }

    private fun shareCallsAsFile() {
        if (calls.isEmpty()) return
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        val text = buildNetworkLogText(calls, dateFormat)
        val file = File(cacheDir, "network_calls.txt")
        file.writeText(text)
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.dataviewer.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Network Calls Log")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share"))
    }

    private fun JSONObject?.toMap(): Map<String, String> {
        if (this == null) return emptyMap()
        return keys().asSequence().associateWith { optString(it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkCallsScreen(
    calls: List<NetworkCall>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onClear: () -> Unit,
    onShare: () -> Unit,
    onCallClick: (NetworkCall) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network (${calls.size})") },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            offset = DpOffset(x = 0.dp, y = 0.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Refresh") },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                onClick = { showMenu = false; onRefresh() }
                            )
                            if (calls.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Share Log") },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                    onClick = { showMenu = false; onShare() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear All") },
                                    leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                                    onClick = { showMenu = false; onClear() }
                                )
                            }
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
        when {
            isLoading -> Box(
                Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            calls.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        "No network calls recorded",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Call startNetworkTracking() to begin",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            else -> LazyColumn(modifier = Modifier.padding(paddingValues)) {
                itemsIndexed(calls) { _, call ->
                    NetworkCallRow(call = call, onClick = { onCallClick(call) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun NetworkCallRow(call: NetworkCall, onClick: () -> Unit) {
    val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    val time = timeFormat.format(Date(call.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        MethodBadge(method = call.method)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = call.url,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${call.duration}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        StatusBadge(status = call.status, error = call.error)
    }
}

@Composable
fun MethodBadge(method: String) {
    val color = when (method.uppercase()) {
        "GET" -> Color(0xFF4CAF50)
        "POST" -> Color(0xFF2196F3)
        "PUT" -> Color(0xFFFF9800)
        "PATCH" -> Color(0xFF9C27B0)
        "DELETE" -> Color(0xFFF44336)
        else -> Color(0xFF607D8B)
    }
    Box(
        modifier = Modifier
            .background(color = color, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = method.uppercase(), color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

private fun prettyJsonForLog(raw: String): String {
    val trimmed = raw.trim()
    return try {
        when {
            trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
            trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
            else -> raw
        }
    } catch (_: Exception) { raw }
}

private fun buildNetworkLogText(calls: List<NetworkCall>, dateFormat: SimpleDateFormat): String {
    val separator = "=".repeat(80)
    return buildString {
        appendLine("Network Calls Log — ${calls.size} record(s)")
        appendLine("Exported: ${dateFormat.format(Date())}")
        appendLine()
        calls.forEachIndexed { index, call ->
            appendLine(separator)
            appendLine("[${index + 1} / ${calls.size}] ${call.method.uppercase()} ${call.url}")
            val statusText = when {
                call.error != null && call.status == null -> "Error: ${call.error}"
                call.status != null -> "${call.status}"
                else -> "—"
            }
            appendLine("Status: $statusText | Duration: ${call.duration}ms | ${dateFormat.format(Date(call.timestamp))}")
            if (call.error != null) appendLine("Error: ${call.error}")
            appendLine()
            appendLine("--- cURL ---")
            append("curl -X ${call.method.uppercase()} '${call.url}'")
            call.requestHeaders.entries.forEach { (k, v) -> append(" \\\n  -H '$k: $v'") }
            if (call.requestBody != null) append(" \\\n  -d '${prettyJsonForLog(call.requestBody)}'")
            appendLine()
            appendLine()
            appendLine("--- Response Headers ---")
            if (call.responseHeaders.isEmpty()) appendLine("(none)")
            else call.responseHeaders.entries.forEach { (k, v) -> appendLine("$k: $v") }
            if (call.responseBody != null) {
                appendLine()
                appendLine("--- Response Body ---")
                appendLine(prettyJsonForLog(call.responseBody))
            }
        }
        appendLine(separator)
    }
}

@Composable
fun StatusBadge(status: Int?, error: String?) {
    val (text, color) = when {
        error != null && status == null -> "ERR" to Color(0xFFF44336)
        status == null -> "..." to Color(0xFF9E9E9E)
        status in 200..299 -> "$status" to Color(0xFF4CAF50)
        status in 300..399 -> "$status" to Color(0xFFFF9800)
        status in 400..499 -> "$status" to Color(0xFFF44336)
        status >= 500 -> "$status" to Color(0xFFB71C1C)
        else -> "$status" to Color(0xFF9E9E9E)
    }
    Box(
        modifier = Modifier
            .background(color = color, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}
