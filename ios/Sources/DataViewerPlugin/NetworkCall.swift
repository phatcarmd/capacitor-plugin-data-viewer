import Foundation

struct NetworkCall: Identifiable {
    let id: String
    let url: String
    let method: String
    let requestHeaders: [String: String]
    let requestBody: String?
    let status: Int?
    let responseHeaders: [String: String]
    let responseBody: String?
    let duration: Int
    let timestamp: TimeInterval
    let error: String?

    init?(dict: [String: Any]) {
        guard let id = dict["id"] as? String, !id.isEmpty,
              let url = dict["url"] as? String else { return nil }
        self.id = id
        self.url = url
        self.method = (dict["method"] as? String ?? "GET").uppercased()
        self.requestHeaders = (dict["requestHeaders"] as? [String: Any] ?? [:])
            .compactMapValues { $0 as? String }
        self.requestBody = dict["requestBody"] as? String
        self.status = (dict["status"] as? NSNumber)?.intValue
        self.responseHeaders = (dict["responseHeaders"] as? [String: Any] ?? [:])
            .compactMapValues { $0 as? String }
        self.responseBody = dict["responseBody"] as? String
        self.duration = (dict["duration"] as? NSNumber)?.intValue ?? 0
        self.timestamp = ((dict["timestamp"] as? NSNumber)?.doubleValue ?? 0) / 1000
        self.error = dict["error"] as? String
    }
}
