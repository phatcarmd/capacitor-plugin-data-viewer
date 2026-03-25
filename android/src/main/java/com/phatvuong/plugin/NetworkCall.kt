package com.phatvuong.plugin

data class NetworkCall(
    val id: String,
    val url: String,
    val method: String,
    val requestHeaders: Map<String, String>,
    val requestBody: String?,
    val status: Int?,
    val responseHeaders: Map<String, String>,
    val responseBody: String?,
    val duration: Long,
    val timestamp: Long,
    val error: String?
)
