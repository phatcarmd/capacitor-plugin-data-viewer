package com.phatvuong.plugin

data class FilterCondition(
    val column: String,
    val operator: String = "=", // =, !=, LIKE, >, <
    val value: String = ""
)
