package com.phatvuong.plugin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun buildShareText(call: NetworkCall): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    val statusText = when {
        call.error != null && call.status == null -> "Error: ${call.error}"
        call.status != null -> "${call.status}"
        else -> "—"
    }
    return buildString {
        appendLine("[${call.method.uppercase()}] ${call.url}")
        appendLine("Status: $statusText | Duration: ${call.duration}ms | ${dateFormat.format(Date(call.timestamp))}")
        if (call.error != null) appendLine("Error: ${call.error}")
        appendLine()

        // cURL command
        appendLine("--- cURL ---")
        append("curl -X ${call.method.uppercase()} '${call.url}'")
        call.requestHeaders.entries.forEach { (k, v) ->
            append(" \\\n  -H '$k: $v'")
        }
        if (call.requestBody != null) {
            val body = prettyJson(call.requestBody)
            append(" \\\n  -d '$body'")
        }
        appendLine()
        appendLine()

        // Response
        appendLine("--- Response Headers ---")
        if (call.responseHeaders.isEmpty()) appendLine("(none)")
        else call.responseHeaders.entries.forEach { (k, v) -> appendLine("$k: $v") }
        if (call.responseBody != null) {
            appendLine()
            appendLine("--- Response Body ---")
            append(prettyJson(call.responseBody))
        }
    }
}

private fun prettyJson(raw: String): String {
    val trimmed = raw.trim()
    return try {
        when {
            trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
            trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
            else -> raw
        }
    } catch (_: Exception) {
        raw
    }
}

class NetworkCallDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val callId = intent.getStringExtra("CALL_ID") ?: return finish()
        val call = NetworkCallStore.calls.value?.find { it.id == callId } ?: return finish()
        setContent { NetworkCallDetailScreen(call = call) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkCallDetailScreen(call: NetworkCall) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(call.method.uppercase()) },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, buildShareText(call))
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share"))
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onPrimary
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
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            DetailSection(title = "Summary") {
                DetailRow(label = "URL", value = call.url)
                DetailRow(label = "Method", value = call.method.uppercase())
                DetailRow(
                    label = "Status",
                    value = when {
                        call.error != null && call.status == null -> "Error: ${call.error}"
                        call.status != null -> "${call.status}"
                        else -> "—"
                    }
                )
                DetailRow(label = "Duration", value = "${call.duration} ms")
                DetailRow(label = "Time", value = dateFormat.format(Date(call.timestamp)))
                if (call.error != null) {
                    DetailRow(label = "Error", value = call.error)
                }
            }

            HorizontalDivider()

            DetailSection(title = "Request") {
                if (call.requestHeaders.isEmpty()) {
                    DetailRow(label = "Headers", value = "(none)")
                } else {
                    call.requestHeaders.entries.forEach { (k, v) ->
                        DetailRow(label = k, value = v, monoLabel = true)
                    }
                }
                if (call.requestBody != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailBodyBlock(label = "Body", body = call.requestBody)
                }
            }

            HorizontalDivider()

            DetailSection(title = "Response") {
                if (call.responseHeaders.isEmpty()) {
                    DetailRow(label = "Headers", value = "(none)")
                } else {
                    call.responseHeaders.entries.forEach { (k, v) ->
                        DetailRow(label = k, value = v, monoLabel = true)
                    }
                }
                if (call.responseBody != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailBodyBlock(label = "Body", body = call.responseBody)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, monoLabel: Boolean = false) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = if (monoLabel) FontFamily.Monospace else null,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .weight(0.65f)
                .clickable {
                    clipboard.setText(AnnotatedString(value))
                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                }
        )
    }
}

@Composable
private fun DetailBodyBlock(label: String, body: String) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val formatted = prettyJson(body)

    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    Text(
        text = formatted,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable {
                clipboard.setText(AnnotatedString(formatted))
                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
            }
            .padding(10.dp)
    )
}
