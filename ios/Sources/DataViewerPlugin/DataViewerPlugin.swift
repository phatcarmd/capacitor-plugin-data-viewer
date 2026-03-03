import Foundation
import Capacitor
import SwiftUI

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(DataViewerPlugin)
public class DataViewerPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "DataViewerPlugin"
    public let jsName = "DataViewer"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "explore", returnType: CAPPluginReturnPromise)
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
        
        call.resolve()
    }
}
