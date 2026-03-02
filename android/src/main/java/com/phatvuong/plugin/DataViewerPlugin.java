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
}
