import SwiftUI

struct NetworkCallsView: View {
    @ObservedObject private var store = NetworkCallStore.shared

    var body: some View {
        Group {
            if store.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if store.calls.isEmpty {
                VStack(spacing: 8) {
                    Text("No network calls recorded")
                        .foregroundColor(.secondary)
                    Text("Call startNetworkTracking() to begin")
                        .font(.caption)
                        .foregroundColor(.secondary)
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
                HStack(spacing: 16) {
                    Button(action: { store.loadFromJS() }) {
                        Image(systemName: "arrow.clockwise")
                    }
                    if !store.calls.isEmpty {
                        Button(action: { store.clear() }) {
                            Image(systemName: "trash")
                        }
                    }
                }
                .foregroundColor(.white)
            }
        }
        .onAppear {
            store.loadFromJS()
        }
    }
}

struct NetworkCallRowView: View {
    let call: NetworkCall

    private var timeString: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm:ss.SSS"
        return formatter.string(from: Date(timeIntervalSince1970: call.timestamp))
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
