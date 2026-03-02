import Foundation

@objc public class DataViewer: NSObject {
    @objc public func echo(_ value: String) -> String {
        print(value)
        return value
    }
}
