package com.phatvuong.plugin;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "DataViewer")
public class DataViewerPlugin extends Plugin {

    private final DataViewer implementation = new DataViewer();

    @PluginMethod
    public void explore(PluginCall call) {
        JSObject ret = new JSObject();
        implementation.explore(getContext());
        call.resolve(ret);
    }

    @PluginMethod
    public void startNetworkTracking(PluginCall call) {
        implementation.startNetworkTracking(getBridge(), call);
    }

    @PluginMethod
    public void stopNetworkTracking(PluginCall call) {
        implementation.stopNetworkTracking(getBridge(), call);
    }
}
