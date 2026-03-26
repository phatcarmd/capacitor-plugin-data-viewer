import Foundation
import WebKit
import SwiftUI

class NetworkCallStore: ObservableObject {
    static let shared = NetworkCallStore()

    @Published var calls: [NetworkCall] = []
    @Published var isLoading = false

    weak var webView: WKWebView?

    func loadFromJS() {
        guard let webView = webView else {
            isLoading = false
            return
        }
        isLoading = true
        DispatchQueue.main.async {
            webView.evaluateJavaScript("window.__dvNetworkCalls || []") { [weak self] result, _ in
                DispatchQueue.main.async {
                    if let raw = result as? [[String: Any]] {
                        self?.calls = raw.compactMap { NetworkCall(dict: $0) }
                    }
                    self?.isLoading = false
                }
            }
        }
    }

    func clear() {
        calls = []
        webView?.evaluateJavaScript("window.__dvNetworkCalls = []; undefined", completionHandler: nil)
    }

    // Shared injection script (identical logic to Android)
    static let injectionScript = """
(function() {
  if (window.__dvNetTracking) return;
  window.__dvNetTracking = true;
  if (!window.__dvNetworkCalls) window.__dvNetworkCalls = [];
  window.__dvNetTrackingEnabled = true;

  var SKIP_EXTENSIONS = /\\.(svg|png|jpe?g|gif|ico|webp|woff2?|ttf|eot|otf|bmp|cur|map|css|js)(\\?.*)?$/i;
  var SKIP_PROTOCOLS = /^(capacitor|ionic|file|chrome-extension):\\/\\//i;

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

  function isBinary(contentType) {
    if (!contentType) return false;
    var ct = contentType.toLowerCase();
    return /^(image|video|audio)\\//.test(ct) ||
      /^font\\//.test(ct) ||
      /application\\/(octet-stream|pdf|zip|x-zip|gzip|x-tar|x-rar|wasm|protobuf|msgpack|cbor)/.test(ct) ||
      /multipart\\/form-data/.test(ct);
  }

  function formatSize(bytes) {
    if (!bytes || isNaN(bytes) || bytes <= 0) return 'unknown size';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / 1048576).toFixed(1) + ' MB';
  }

  function binarySummary(contentType, contentLength) {
    var size = contentLength ? formatSize(parseInt(contentLength)) : 'unknown size';
    return '[binary: ' + (contentType || 'unknown') + ', ' + size + ']';
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
      if (b) {
        if (typeof b === 'string') {
          reqBody = b;
        } else if (b instanceof FormData) {
          reqBody = '[multipart/form-data]';
        } else if (b instanceof Blob || b instanceof ArrayBuffer) {
          reqBody = '[binary: ' + formatSize(b.size || b.byteLength) + ']';
        } else {
          reqBody = JSON.stringify(b);
        }
      }
    } catch(e) {}
    var startTime = Date.now();
    var callId = Math.random().toString(36).substr(2, 9);

    return _fetch.apply(this, arguments).then(function(response) {
      var status = response.status;
      var resHeaders = headersToObj(response.headers);
      var contentType = response.headers.get('content-type') || '';
      var contentLength = response.headers.get('content-length');
      var duration = Date.now() - startTime;
      if (isBinary(contentType)) {
        sendLog({
          id: callId, url: url, method: method,
          requestHeaders: reqHeaders, requestBody: reqBody,
          status: status, responseHeaders: resHeaders,
          responseBody: binarySummary(contentType, contentLength),
          duration: duration, timestamp: startTime, error: null
        });
      } else {
        response.clone().text().then(function(body) {
          sendLog({
            id: callId, url: url, method: method,
            requestHeaders: reqHeaders, requestBody: reqBody,
            status: status, responseHeaders: resHeaders,
            responseBody: truncate(body),
            duration: duration, timestamp: startTime, error: null
          });
        }).catch(function() {
          sendLog({
            id: callId, url: url, method: method,
            requestHeaders: reqHeaders, requestBody: reqBody,
            status: status, responseHeaders: resHeaders, responseBody: null,
            duration: duration, timestamp: startTime, error: null
          });
        });
      }
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
        try {
          if (typeof body === 'string') {
            _reqBody = body;
          } else if (body instanceof FormData) {
            _reqBody = '[multipart/form-data]';
          } else if (body instanceof Blob || body instanceof ArrayBuffer) {
            _reqBody = '[binary: ' + formatSize(body.size || body.byteLength) + ']';
          } else {
            _reqBody = JSON.stringify(body);
          }
        } catch(e) {}
      }
      xhr.addEventListener('loadend', function() {
        var resHeaders = {};
        try {
          var raw = xhr.getAllResponseHeaders();
          raw.trim().split('\\r\\n').forEach(function(line) {
            var idx = line.indexOf(': ');
            if (idx > 0) resHeaders[line.substring(0, idx)] = line.substring(idx + 2);
          });
        } catch(e) {}
        var contentType = xhr.getResponseHeader('content-type') || '';
        var contentLength = xhr.getResponseHeader('content-length');
        var resBody = (xhr.responseType === 'blob' || xhr.responseType === 'arraybuffer' || isBinary(contentType))
          ? binarySummary(contentType, contentLength)
          : truncate(xhr.responseText);
        sendLog({
          id: callId, url: _url, method: _method,
          requestHeaders: _reqHeaders, requestBody: _reqBody,
          status: xhr.status || null,
          responseHeaders: resHeaders,
          responseBody: resBody,
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
"""
}
