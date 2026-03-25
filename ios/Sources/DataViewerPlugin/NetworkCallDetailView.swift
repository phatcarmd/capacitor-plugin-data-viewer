import SwiftUI
import UIKit

struct NetworkCallDetailView: View {
    let call: NetworkCall

    private static let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd HH:mm:ss.SSS"
        return f
    }()

    private var dateString: String {
        NetworkCallDetailView.dateFormatter.string(from: Date(timeIntervalSince1970: call.timestamp))
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                // Summary
                DetailSectionView(title: "Summary") {
                    DetailRowView(label: "URL",      value: call.url)
                    DetailRowView(label: "Method",   value: call.method.uppercased())
                    DetailRowView(label: "Status",   value: statusText)
                    DetailRowView(label: "Duration", value: "\(call.duration) ms")
                    DetailRowView(label: "Time",     value: dateString)
                    if let err = call.error {
                        DetailRowView(label: "Error", value: err)
                    }
                }

                Divider()

                // Request
                DetailSectionView(title: "Request") {
                    if call.requestHeaders.isEmpty {
                        DetailRowView(label: "Headers", value: "(none)")
                    } else {
                        ForEach(call.requestHeaders.sorted(by: { $0.key < $1.key }), id: \.key) { k, v in
                            DetailRowView(label: k, value: v, monoLabel: true)
                        }
                    }
                    if let body = call.requestBody {
                        DetailBodyView(label: "Body", rawBody: body)
                    }
                }

                Divider()

                // Response
                DetailSectionView(title: "Response") {
                    if call.responseHeaders.isEmpty {
                        DetailRowView(label: "Headers", value: "(none)")
                    } else {
                        ForEach(call.responseHeaders.sorted(by: { $0.key < $1.key }), id: \.key) { k, v in
                            DetailRowView(label: k, value: v, monoLabel: true)
                        }
                    }
                    if let body = call.responseBody {
                        DetailBodyView(label: "Body", rawBody: body)
                    }
                }

                Spacer(minLength: 24)
            }
        }
        .navigationTitle(call.method.uppercased())
        .navigationBarTitleDisplayMode(.inline)
    }

    private var statusText: String {
        if let err = call.error, call.status == nil { return "Error: \(err)" }
        if let s = call.status { return "\(s)" }
        return "—"
    }
}

// MARK: - Section

private struct DetailSectionView<Content: View>: View {
    let title: String
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundColor(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(Color(.secondarySystemBackground))

            VStack(alignment: .leading, spacing: 4) {
                content()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
        }
    }
}

// MARK: - Row

private struct DetailRowView: View {
    let label: String
    let value: String
    var monoLabel: Bool = false

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Text(label)
                .font(monoLabel ? .system(.caption, design: .monospaced) : .caption)
                .foregroundColor(.secondary)
                .frame(width: 110, alignment: .leading)
                .fixedSize(horizontal: false, vertical: true)

            Text(value)
                .font(.system(.caption, design: .monospaced))
                .frame(maxWidth: .infinity, alignment: .leading)
                .fixedSize(horizontal: false, vertical: true)
                .onTapGesture { copyToClipboard(value) }
        }
        .padding(.vertical, 2)
    }
}

// MARK: - Body block

private struct DetailBodyView: View {
    let label: String
    let rawBody: String

    private var formatted: String { prettyJson(rawBody) }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)

            Text(formatted)
                .font(.system(.caption, design: .monospaced))
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(10)
                .background(Color(.secondarySystemBackground))
                .cornerRadius(6)
                .onTapGesture { copyToClipboard(formatted) }
        }
        .padding(.top, 4)
    }
}

// MARK: - Helpers

private func copyToClipboard(_ text: String) {
    UIPasteboard.general.string = text
    let feedback = UINotificationFeedbackGenerator()
    feedback.notificationOccurred(.success)
}

private func prettyJson(_ raw: String) -> String {
    let trimmed = raw.trimmingCharacters(in: .whitespaces)
    guard trimmed.hasPrefix("{") || trimmed.hasPrefix("["),
          let data = trimmed.data(using: .utf8),
          let obj = try? JSONSerialization.jsonObject(with: data),
          let pretty = try? JSONSerialization.data(withJSONObject: obj, options: .prettyPrinted),
          let result = String(data: pretty, encoding: .utf8)
    else { return raw }
    return result
}
