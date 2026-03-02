package com.phatvuong.plugin

import android.content.Context
import android.content.Intent
import com.getcapacitor.Logger

class DataViewer {
    fun echo(value: String?): String? {
        Logger.info("Echo1", value)
        return value
    }

    fun explore(context: Context) {
        val intent = Intent(context, DataExplorerActivity::class.java)
        context.startActivity(intent)

    }
}
