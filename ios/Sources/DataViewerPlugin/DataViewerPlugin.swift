import Foundation
import Capacitor
import SwiftUI

@objc(DataViewerPlugin)
public class DataViewerPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "DataViewerPlugin"
    public let jsName = "DataViewer"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "explore",              returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "startNetworkTracking", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stopNetworkTracking",  returnType: CAPPluginReturnPromise),
    ]
    private let implementation = DataViewer()

    @objc func explore(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            let dbListView = DataExplorerView(onDismiss: {
                self.bridge?.viewController?.dismiss(animated: true)
            })

            let hostingController = UIHostingController(rootView: dbListView)
            hostingController.modalPresentationStyle = .fullScreen

            self.bridge?.viewController?.present(hostingController, animated: true) {
                call.resolve()
            }
        }
    }

    @objc func startNetworkTracking(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            NetworkCallStore.shared.webView = self.bridge?.webView
            self.bridge?.webView?.evaluateJavaScript(NetworkCallStore.injectionScript) { [weak self] _, _ in
                // Re-enable in case stopNetworkTracking was called previously
                self?.bridge?.webView?.evaluateJavaScript(
                    "window.__dvNetTrackingEnabled = true; undefined",
                    completionHandler: nil
                )
                call.resolve()
            }
        }
    }

    @objc func stopNetworkTracking(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            self.bridge?.webView?.evaluateJavaScript(
                "window.__dvNetTrackingEnabled = false; undefined"
            ) { _, _ in
                call.resolve()
            }
        }
    }
}
