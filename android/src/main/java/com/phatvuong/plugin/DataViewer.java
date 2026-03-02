package com.phatvuong.plugin;

import com.getcapacitor.Logger;

public class DataViewer {

    public String echo(String value) {
        Logger.info("Echo", value);
        return value;
    }
}
