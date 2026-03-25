package com.phatvuong.plugin

import android.content.Context
import android.content.Intent
import com.getcapacitor.Bridge
import com.getcapacitor.PluginCall
import java.lang.ref.WeakReference

class DataViewer {

    companion object {
        // WeakReference so we don't prevent GC of the bridge/activity
        var bridgeRef: WeakReference<Bridge>? = null
    }

    fun explore(context: Context) {
        val intent = Intent(context, DataExplorerActivity::class.java)
        context.startActivity(intent)
    }

    fun startNetworkTracking(bridge: Bridge, call: PluginCall) {
        bridgeRef = WeakReference(bridge)
        bridge.activity.runOnUiThread {
            bridge.webView.evaluateJavascript(NetworkBridgeInterface.INJECTION_SCRIPT) {
                // Re-enable in case stopNetworkTracking was called previously
                bridge.webView.evaluateJavascript("window.__dvNetTrackingEnabled = true; void 0", null)
                call.resolve()
            }
        }
    }

    fun stopNetworkTracking(bridge: Bridge, call: PluginCall) {
        bridge.activity.runOnUiThread {
            bridge.webView.evaluateJavascript("window.__dvNetTrackingEnabled = false; void 0") {
                call.resolve()
            }
        }
    }
}
