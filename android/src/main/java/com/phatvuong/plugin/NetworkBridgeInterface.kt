package com.phatvuong.plugin

import android.webkit.JavascriptInterface
import org.json.JSONObject

class NetworkBridgeInterface {

    @JavascriptInterface
    fun log(json: String) {
        try {
            val obj = JSONObject(json)
            val reqHeadersObj = obj.optJSONObject("requestHeaders")
            val resHeadersObj = obj.optJSONObject("responseHeaders")

            val reqHeaders = mutableMapOf<String, String>()
            reqHeadersObj?.keys()?.forEach { key -> reqHeaders[key] = reqHeadersObj.optString(key) }

            val resHeaders = mutableMapOf<String, String>()
            resHeadersObj?.keys()?.forEach { key -> resHeaders[key] = resHeadersObj.optString(key) }

            val call = NetworkCall(
                id = obj.optString("id"),
                url = obj.optString("url"),
                method = obj.optString("method", "GET"),
                requestHeaders = reqHeaders,
                requestBody = obj.optString("requestBody").takeIf { it.isNotEmpty() },
                status = obj.optInt("status", 0).takeIf { it != 0 },
                responseHeaders = resHeaders,
                responseBody = obj.optString("responseBody").takeIf { it.isNotEmpty() },
                duration = obj.optLong("duration"),
                timestamp = obj.optLong("timestamp"),
                error = obj.optString("error").takeIf { it.isNotEmpty() && it != "null" }
            )
            NetworkCallStore.add(call)
        } catch (_: Exception) {
        }
    }

    companion object {
        // language=JavaScript
        val INJECTION_SCRIPT = """
(function() {
  if (window.__dvNetTracking) return;
  window.__dvNetTracking = true;

  if (!window.__dvNetworkCalls) window.__dvNetworkCalls = [];
  window.__dvNetTrackingEnabled = true;

  var SKIP_EXTENSIONS = /\.(svg|png|jpe?g|gif|ico|webp|woff2?|ttf|eot|otf|bmp|cur|map|css|js)(\?.*)?$/i;
  var SKIP_PROTOCOLS = /^(capacitor|ionic|file|chrome-extension):\/\//i;

  function shouldSkip(url) {
    if (!url) return true;
    if (SKIP_PROTOCOLS.test(url)) return true;
    var path = url.split('?')[0].split('#')[0];
    return SKIP_EXTENSIONS.test(path);
  }

  function sendLog(data) {
    if (!window.__dvNetTrackingEnabled) return;
    if (shouldSkip(data.url)) return;
    try {
      window.__dvNetworkCalls.unshift(data);
      if (window.__dvNetworkCalls.length > 200) window.__dvNetworkCalls.pop();
    } catch(e) {}
  }

  function headersToObj(headers) {
    var obj = {};
    if (!headers) return obj;
    if (typeof headers.forEach === 'function') {
      headers.forEach(function(v, k) { obj[k] = v; });
    } else if (typeof headers === 'object') {
      Object.keys(headers).forEach(function(k) { obj[k] = headers[k]; });
    }
    return obj;
  }

  function truncate(str) {
    if (!str) return null;
    return str.length > 50000 ? str.substring(0, 50000) + '...[truncated]' : str;
  }

  var _fetch = window.fetch;
  window.fetch = function(input, init) {
    var url = (typeof input === 'string') ? input : (input && input.url) || '';
    var method = ((init && init.method) || (input && typeof input === 'object' && input.method) || 'GET').toUpperCase();
    var reqHeaders = {};
    try {
      var h = (init && init.headers) || (input && typeof input === 'object' && input.headers);
      if (h) reqHeaders = headersToObj(h instanceof Headers ? h : new Headers(h));
    } catch(e) {}
    var reqBody = null;
    try {
      var b = init && init.body;
      if (b) reqBody = typeof b === 'string' ? b : JSON.stringify(b);
    } catch(e) {}
    var startTime = Date.now();
    var callId = Math.random().toString(36).substr(2, 9);

    return _fetch.apply(this, arguments).then(function(response) {
      var status = response.status;
      var resHeaders = headersToObj(response.headers);
      var cloned = response.clone();
      cloned.text().then(function(body) {
        sendLog({
          id: callId, url: url, method: method,
          requestHeaders: reqHeaders, requestBody: reqBody,
          status: status, responseHeaders: resHeaders,
          responseBody: truncate(body),
          duration: Date.now() - startTime, timestamp: startTime, error: null
        });
      }).catch(function() {
        sendLog({
          id: callId, url: url, method: method,
          requestHeaders: reqHeaders, requestBody: reqBody,
          status: status, responseHeaders: resHeaders, responseBody: null,
          duration: Date.now() - startTime, timestamp: startTime, error: null
        });
      });
      return response;
    }, function(err) {
      sendLog({
        id: callId, url: url, method: method,
        requestHeaders: reqHeaders, requestBody: reqBody,
        status: null, responseHeaders: {}, responseBody: null,
        duration: Date.now() - startTime, timestamp: startTime,
        error: err ? (err.message || String(err)) : 'Unknown error'
      });
      throw err;
    });
  };

  var _XHR = window.XMLHttpRequest;
  function PatchedXHR() {
    var xhr = new _XHR();
    var _url = '', _method = 'GET', _reqHeaders = {}, _reqBody = null, _startTime = 0;
    var callId = Math.random().toString(36).substr(2, 9);

    var origOpen = xhr.open.bind(xhr);
    xhr.open = function(method, url) {
      _method = method;
      _url = url;
      return origOpen.apply(xhr, arguments);
    };

    var origSetHeader = xhr.setRequestHeader.bind(xhr);
    xhr.setRequestHeader = function(name, value) {
      _reqHeaders[name] = value;
      return origSetHeader.apply(xhr, arguments);
    };

    var origSend = xhr.send.bind(xhr);
    xhr.send = function(body) {
      _startTime = Date.now();
      if (body) {
        try { _reqBody = typeof body === 'string' ? body : JSON.stringify(body); } catch(e) {}
      }
      xhr.addEventListener('loadend', function() {
        var resHeaders = {};
        try {
          var raw = xhr.getAllResponseHeaders();
          raw.trim().split('\r\n').forEach(function(line) {
            var idx = line.indexOf(': ');
            if (idx > 0) resHeaders[line.substring(0, idx)] = line.substring(idx + 2);
          });
        } catch(e) {}
        sendLog({
          id: callId, url: _url, method: _method,
          requestHeaders: _reqHeaders, requestBody: _reqBody,
          status: xhr.status || null,
          responseHeaders: resHeaders,
          responseBody: truncate(xhr.responseText),
          duration: Date.now() - _startTime, timestamp: _startTime,
          error: xhr.status === 0 ? 'Network error' : null
        });
      });
      return origSend.apply(xhr, arguments);
    };

    return xhr;
  }
  PatchedXHR.prototype = _XHR.prototype;
  window.XMLHttpRequest = PatchedXHR;

  // Patch CapacitorHttp.request() — for apps using CapacitorHttp directly
  (function() {
    var cap = window.Capacitor;
    if (!cap || typeof cap.nativePromise !== 'function') return;
    var _np = cap.nativePromise;
    cap.nativePromise = function(pluginName, methodName, options) {
      if (pluginName === 'CapacitorHttp' && options && options.url) {
        var startTime = Date.now();
        var callId = Math.random().toString(36).substr(2, 9);
        var method = (options.method || 'GET').toUpperCase();
        var reqBody = null;
        try {
          if (options.data != null) {
            reqBody = typeof options.data === 'string' ? options.data : JSON.stringify(options.data);
          }
        } catch(e) {}
        return _np.apply(cap, arguments).then(function(response) {
          var resBody = null;
          try { resBody = typeof response.data === 'string' ? response.data : JSON.stringify(response.data); } catch(e) {}
          sendLog({
            id: callId, url: options.url, method: method,
            requestHeaders: options.headers || {}, requestBody: reqBody,
            status: response.status, responseHeaders: response.headers || {},
            responseBody: truncate(resBody),
            duration: Date.now() - startTime, timestamp: startTime, error: null
          });
          return response;
        }, function(err) {
          sendLog({
            id: callId, url: options.url, method: method,
            requestHeaders: options.headers || {}, requestBody: reqBody,
            status: null, responseHeaders: {}, responseBody: null,
            duration: Date.now() - startTime, timestamp: startTime,
            error: err ? (err.message || String(err)) : 'Unknown error'
          });
          throw err;
        });
      }
      return _np.apply(cap, arguments);
    };
  })();
})();
""".trimIndent()
    }
}
