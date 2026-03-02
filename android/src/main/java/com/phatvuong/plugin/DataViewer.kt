package com.phatvuong.plugin

import android.content.Context
import android.content.Intent

class DataViewer {
    fun explore(context: Context) {
        val intent = Intent(context, DataExplorerActivity::class.java)
        context.startActivity(intent)
    }
}
