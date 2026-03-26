import SwiftUI

extension URL: @retroactive Identifiable {
    public var id: String { absoluteString }
}

struct NetworkCallsView: View {
    @ObservedObject private var store = NetworkCallStore.shared
    @State private var exportFileUrl: URL? = nil

    private static let exportDateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd HH:mm:ss.SSS"
        return f
    }()

    var body: some View {
        Group {
            if store.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if store.calls.isEmpty {
                VStack(spacing: 8) {
                    Image(systemName: "wifi.slash")
                        .font(.system(size: 36))
                        .foregroundColor(.secondary)
                        .opacity(0.4)
                        .padding(.bottom, 4)
                    Text("No network calls recorded")
                        .foregroundColor(.secondary)
                    Text("Call startNetworkTracking() to begin")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .opacity(0.6)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List(store.calls) { call in
                    NavigationLink(destination: NetworkCallDetailView(call: call)) {
                        NetworkCallRowView(call: call)
                    }
                }
                .listStyle(PlainListStyle())
            }
        }
        .navigationTitle("Network (\(store.calls.count))")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    Button(action: { store.loadFromJS() }) {
                        Label("Refresh", systemImage: "arrow.clockwise")
                    }
                    if !store.calls.isEmpty {
                        Button(action: { exportToFile() }) {
                            Label("Share Log", systemImage: "square.and.arrow.up")
                        }
                        Button(action: { store.clear() }) {
                            Label("Clear All", systemImage: "trash")
                                .foregroundColor(.red)
                        }
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .foregroundColor(.white)
                }
            }
        }
        .sheet(item: $exportFileUrl) { url in
            ShareSheet(activityItems: [url])
        }
        .onAppear {
            store.loadFromJS()
        }
    }

    private func exportToFile() {
        let calls = store.calls
        guard !calls.isEmpty else { return }
        DispatchQueue.global(qos: .userInitiated).async {
            let text = buildNetworkLogText(calls)
            let fileUrl = FileManager.default.temporaryDirectory
                .appendingPathComponent("network_calls.txt")
            try? text.write(to: fileUrl, atomically: true, encoding: .utf8)
            DispatchQueue.main.async {
                exportFileUrl = fileUrl
            }
        }
    }

    private func buildNetworkLogText(_ calls: [NetworkCall]) -> String {
        let fmt = NetworkCallsView.exportDateFormatter
        let separator = String(repeating: "=", count: 80)
        var lines: [String] = []
        lines.append("Network Calls Log — \(calls.count) record(s)")
        lines.append("Exported: \(fmt.string(from: Date()))")
        lines.append("")
        for (index, call) in calls.enumerated() {
            lines.append(separator)
            lines.append("[\(index + 1) / \(calls.count)] \(call.method.uppercased()) \(call.url)")
            let statusText: String = {
                if let err = call.error, call.status == nil { return "Error: \(err)" }
                if let s = call.status { return "\(s)" }
                return "—"
            }()
            lines.append("Status: \(statusText) | Duration: \(call.duration)ms | \(fmt.string(from: Date(timeIntervalSince1970: call.timestamp)))")
            if let err = call.error { lines.append("Error: \(err)") }
            lines.append("")
            lines.append("--- cURL ---")
            var curl = "curl -X \(call.method.uppercased()) '\(call.url)'"
            call.requestHeaders.sorted(by: { $0.key < $1.key }).forEach { curl += " \\\n  -H '\($0.key): \($0.value)'" }
            if let body = call.requestBody { curl += " \\\n  -d '\(prettyJson(body))'" }
            lines.append(curl)
            lines.append("")
            lines.append("--- Response Headers ---")
            if call.responseHeaders.isEmpty {
                lines.append("(none)")
            } else {
                call.responseHeaders.sorted(by: { $0.key < $1.key }).forEach { lines.append("\($0.key): \($0.value)") }
            }
            if let body = call.responseBody {
                lines.append("")
                lines.append("--- Response Body ---")
                lines.append(prettyJson(body))
            }
            lines.append("")
        }
        lines.append(separator)
        return lines.joined(separator: "\n")
    }
}

struct NetworkCallRowView: View {
    let call: NetworkCall

    private static let timeFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "HH:mm:ss.SSS"
        return f
    }()

    private var timeString: String {
        NetworkCallRowView.timeFormatter.string(from: Date(timeIntervalSince1970: call.timestamp))
    }

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            MethodBadgeView(method: call.method)
            VStack(alignment: .leading, spacing: 3) {
                Text(call.url)
                    .font(.system(.footnote, design: .monospaced))
                    .lineLimit(5)
                HStack(spacing: 8) {
                    Text(timeString)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    Text("\(call.duration)ms")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
            }
            Spacer(minLength: 4)
            StatusBadgeView(status: call.status, error: call.error)
        }
        .padding(.vertical, 4)
    }
}

struct MethodBadgeView: View {
    let method: String

    private var color: Color {
        switch method.uppercased() {
        case "GET":    return Color(red: 0.298, green: 0.686, blue: 0.314)
        case "POST":   return Color(red: 0.129, green: 0.588, blue: 0.953)
        case "PUT":    return Color(red: 1.0,   green: 0.596, blue: 0.0)
        case "PATCH":  return Color(red: 0.612, green: 0.153, blue: 0.690)
        case "DELETE": return Color(red: 0.957, green: 0.263, blue: 0.212)
        default:       return Color(red: 0.376, green: 0.490, blue: 0.545)
        }
    }

    var body: some View {
        Text(method.uppercased())
            .font(.system(size: 11, weight: .semibold, design: .monospaced))
            .foregroundColor(.white)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(color)
            .cornerRadius(4)
    }
}

struct StatusBadgeView: View {
    let status: Int?
    let error: String?

    private var info: (String, Color) {
        switch true {
        case error != nil && status == nil:
            return ("ERR", Color(red: 0.957, green: 0.263, blue: 0.212))
        case status == nil:
            return ("...", .gray)
        case (200...299).contains(status!):
            return ("\(status!)", Color(red: 0.298, green: 0.686, blue: 0.314))
        case (300...399).contains(status!):
            return ("\(status!)", Color(red: 1.0, green: 0.596, blue: 0.0))
        case (400...499).contains(status!):
            return ("\(status!)", Color(red: 0.957, green: 0.263, blue: 0.212))
        case status! >= 500:
            return ("\(status!)", Color(red: 0.718, green: 0.110, blue: 0.110))
        default:
            return ("\(status!)", .gray)
        }
    }

    var body: some View {
        let (label, color) = info
        Text(label)
            .font(.system(size: 11, weight: .semibold, design: .monospaced))
            .foregroundColor(.white)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(color)
            .cornerRadius(4)
    }
}
