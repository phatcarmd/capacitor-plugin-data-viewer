//
//  PreferenceDetailScreen.swift
//  CapacitorPluginDataViewer
//
//  Created by cmd on 8/3/26.
//

import Foundation
import SwiftUI

struct PreferenceDetailScreen: View {
    let fileUrl: URL
    @State private var rows: [[String]] = []
    @State private var columns = ["Key", "Value", "Type"]
    @State private var showActionSheet = false
    @State private var selectedCellText = ""
    @State private var selectedRowIndex: Int?
    @State private var showEditorSheet = false
    @State private var editorMode: PreferenceEditorMode = .create
    @State private var editingOriginalKey: String?
    @State private var draftKey = ""
    @State private var draftValue = ""
    @State private var draftType = "String"
    @State private var showErrorAlert = false
    @State private var errorMessage = ""
    
    private let repository = DatabaseRepository.shared
    private let columnWidth: CGFloat = 150
    private let editableTypes = ["String", "Int", "Double", "Bool", "Data", "Dictionary", "Array"]

    var body: some View {
        VStack(spacing: 0) {
            if rows.isEmpty {
                Text("No data or unable to read file")
                    .foregroundColor(.gray)
                    .padding()
            } else {
                ScrollView(.horizontal) {
                    ScrollView(.vertical) {
                        LazyVStack(alignment: .leading, spacing: 0) {
                            // Header
                            headerView
                            Divider()
                            
                            // Rows
                            ForEach(rows.indices, id: \.self) { index in
                                rowView(rows[index], index: index)
                                Divider()
                            }
                        }
                    }
                }
            }
        }
        .navigationBarTitle(fileUrl.deletingPathExtension().lastPathComponent, displayMode: .inline)
        .navigationBarItems(trailing: Button(action: beginCreate) {
            Image(systemName: "plus")
        })
        .actionSheet(isPresented: $showActionSheet) {
            let canOperateOnRow = selectedRowIndex != nil
            return ActionSheet(
                title: Text("Cell Options"),
                message: Text(selectedCellText),
                buttons: [
                    .default(Text("Copy")) {
                        UIPasteboard.general.string = selectedCellText
                    },
                    .default(Text("Edit")) {
                        if canOperateOnRow {
                            beginEditSelectedRow()
                        }
                    },
                    .destructive(Text("Delete")) {
                        if canOperateOnRow {
                            deleteSelectedRow()
                        }
                    },
                    .cancel(Text("Close"))
                ]
            )
        }
        .sheet(isPresented: $showEditorSheet) {
            NavigationView {
                Form {
                    Section(header: Text("Field")) {
                        TextField("Key", text: $draftKey)
                            .autocapitalization(.none)
                            .disableAutocorrection(true)
                        TextField("Value", text: $draftValue)
                            .keyboardType(keyboardType(for: draftType))
                        Picker("Type", selection: $draftType) {
                            ForEach(editableTypes, id: \.self) { type in
                                Text(type).tag(type)
                            }
                        }

                        if let validationError = draftValidationError {
                            Text(validationError)
                                .font(.footnote)
                                .foregroundColor(.red)
                        }
                    }
                }
                .navigationBarTitle(editorMode == .create ? "Add Entry" : "Edit Entry", displayMode: .inline)
                .navigationBarItems(
                    leading: Button("Cancel") {
                        showEditorSheet = false
                    }
                    .foregroundColor(.white),
                    trailing: Group {
                        if isDraftValid {
                            Button("Save") {
                                saveEditorChanges()
                            }
                            .foregroundColor(.white)
                        }
                    }
                )
            }
        }
        .alert(isPresented: $showErrorAlert) {
            Alert(title: Text("Cannot Save"), message: Text(errorMessage), dismissButton: .default(Text("OK")))
        }
        .onAppear {
            loadRows()
        }
    }

    var headerView: some View {
        HStack(spacing: 0) {
            ForEach(columns, id: \.self) { col in
                VStack(alignment: .leading, spacing: 0) {
                    Text(col)
                        .font(.caption.bold())
                        .frame(width: columnWidth - 16, alignment: .leading)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 8)

                    Spacer(minLength: 0)
                }
                .frame(width: columnWidth)
                .background(Color(.systemGray6))
                .overlay(
                    Rectangle()
                        .frame(width: 0.5)
                        .foregroundColor(Color(.systemGray4)),
                    alignment: .trailing
                )
            }
        }
        .overlay(
            Rectangle()
                .frame(height: 0.5)
                .foregroundColor(Color(.systemGray4)),
            alignment: .bottom
        )
    }

    func rowView(_ row: [String], index: Int) -> some View {
        HStack(spacing: 0) {
            ForEach(0..<columns.count, id: \.self) { colIndex in
                VStack(alignment: .leading, spacing: 0) {
                    Text(row.indices.contains(colIndex) ? row[colIndex] : "")
                        .font(.system(size: 12))
                        .lineLimit(nil)
                        .multilineTextAlignment(.leading)
                        .fixedSize(horizontal: false, vertical: true)
                        .frame(width: columnWidth - 16, alignment: .topLeading)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 8)
                    
                    Spacer(minLength: 0)
                }
                .frame(width: columnWidth)
                .frame(minHeight: 40)
                .background(index % 2 == 0 ? Color(.systemBackground) : Color(.secondarySystemBackground))
                .overlay(
                    Rectangle()
                        .frame(width: 0.5)
                        .foregroundColor(Color(.systemGray4)),
                    alignment: .trailing
                )
                .onTapGesture {
                    self.selectedCellText = row.indices.contains(colIndex) ? row[colIndex] : ""
                    self.selectedRowIndex = index
                    self.showActionSheet = true
                }
            }
        }
    }

    private func loadRows() {
        let data = repository.getPreferencesData(from: fileUrl)
        self.rows = data.rows
    }

    private func beginCreate() {
        editorMode = .create
        editingOriginalKey = nil
        draftKey = ""
        draftValue = ""
        draftType = "String"
        showEditorSheet = true
    }

    private func beginEditSelectedRow() {
        guard let index = selectedRowIndex, rows.indices.contains(index) else { return }
        let row = rows[index]
        let key = row.indices.contains(0) ? row[0] : ""
        let type = editableTypes.contains(row.indices.contains(2) ? row[2] : "") ? row[2] : "String"
        let dict = repository.getPreferencesDictionary(from: fileUrl)

        editorMode = .edit
        editingOriginalKey = key
        draftKey = key
        draftType = type
        draftValue = displayValueForEditor(dict[key], type: type, fallback: row.indices.contains(1) ? row[1] : "")
        showEditorSheet = true
    }

    private func deleteSelectedRow() {
        guard let index = selectedRowIndex, rows.indices.contains(index) else { return }
        let key = rows[index][0]
        var dict = repository.getPreferencesDictionary(from: fileUrl)
        dict.removeValue(forKey: key)

        if repository.savePreferencesData(dict, to: fileUrl) {
            loadRows()
        } else {
            showSaveError("Failed to delete key '\(key)'.")
        }
    }

    private func saveEditorChanges() {
        let trimmedKey = normalizedDraftKey
        guard !trimmedKey.isEmpty else {
            showSaveError("Key is required.")
            return
        }

        guard let typedValue = typedValue(from: draftValue, type: draftType) else {
            showSaveError("Value does not match selected type '\(draftType)'.")
            return
        }

        var dict = repository.getPreferencesDictionary(from: fileUrl)

        if editorMode == .edit, let oldKey = editingOriginalKey, oldKey != trimmedKey {
            dict.removeValue(forKey: oldKey)
        }

        if editorMode == .create && dict[trimmedKey] != nil {
            showSaveError("Key '\(trimmedKey)' already exists.")
            return
        }

        if editorMode == .edit,
            let oldKey = editingOriginalKey,
            oldKey != trimmedKey,
            dict[trimmedKey] != nil {
            showSaveError("Key '\(trimmedKey)' already exists.")
            return
        }

        dict[trimmedKey] = typedValue

        if repository.savePreferencesData(dict, to: fileUrl) {
            showEditorSheet = false
            loadRows()
        } else {
            showSaveError("Failed to save changes to preferences file.")
        }
    }

    private func typedValue(from text: String, type: String) -> Any? {
        let normalizedText = text.trimmingCharacters(in: .whitespacesAndNewlines)

        switch type {
        case "String":
            return text
        case "Int":
            return Int(normalizedText)
        case "Double":
            return Double(normalizedText)
        case "Bool":
            let normalized = normalizedText.lowercased()
            if normalized == "true" || normalized == "1" { return true }
            if normalized == "false" || normalized == "0" { return false }
            return nil
        case "Data":
            return Data(base64Encoded: normalizedText)
        case "Dictionary":
            guard let data = normalizedText.data(using: .utf8),
                let json = try? JSONSerialization.jsonObject(with: data, options: []),
                let dict = json as? [String: Any] else {
                return nil
            }
            return dict
        case "Array":
            guard let data = normalizedText.data(using: .utf8),
                let json = try? JSONSerialization.jsonObject(with: data, options: []),
                let array = json as? [Any] else {
                return nil
            }
            return array
        default:
            return text
        }
    }

    private func displayValueForEditor(_ rawValue: Any?, type: String, fallback: String) -> String {
        guard let rawValue = rawValue else { return fallback }

        switch type {
        case "Data":
            if let data = rawValue as? Data {
                return data.base64EncodedString()
            }
            return fallback
        case "Dictionary":
            if JSONSerialization.isValidJSONObject(rawValue),
                let data = try? JSONSerialization.data(withJSONObject: rawValue, options: [.prettyPrinted]),
                let jsonText = String(data: data, encoding: .utf8) {
                return jsonText
            }
            return fallback
        case "Array":
            if JSONSerialization.isValidJSONObject(rawValue),
                let data = try? JSONSerialization.data(withJSONObject: rawValue, options: [.prettyPrinted]),
                let jsonText = String(data: data, encoding: .utf8) {
                return jsonText
            }
            return fallback
        default:
            return fallback
        }
    }

    private var normalizedDraftKey: String {
        draftKey.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var draftValidationError: String? {
        if normalizedDraftKey.isEmpty {
            return "Key is required."
        }

        let isDuplicateKeyInCreate = editorMode == .create && rows.contains { $0.count > 0 && $0[0] == normalizedDraftKey }
        if isDuplicateKeyInCreate {
            return "Key '\(normalizedDraftKey)' already exists."
        }

        if editorMode == .edit,
           let oldKey = editingOriginalKey,
           oldKey != normalizedDraftKey,
           rows.contains(where: { $0.count > 0 && $0[0] == normalizedDraftKey }) {
            return "Key '\(normalizedDraftKey)' already exists."
        }

        if typedValue(from: draftValue, type: draftType) == nil {
            switch draftType {
            case "Int":
                return "Value must be a valid integer (e.g. 10, -4)."
            case "Double":
                return "Value must be a valid decimal number (e.g. 3.14)."
            case "Bool":
                return "Bool accepts: true, false, 1, 0."
            case "Data":
                return "Data must be a valid Base64 string."
            case "Dictionary":
                return "Dictionary must be a valid JSON object, e.g. {\"a\":1}."
            case "Array":
                return "Array must be a valid JSON array, e.g. [1, \"a\", true]."
            default:
                return "Value does not match selected type '\(draftType)'."
            }
        }

        return nil
    }

    private var isDraftValid: Bool {
        draftValidationError == nil
    }

    private func keyboardType(for type: String) -> UIKeyboardType {
        switch type {
        case "Int":
            return .numberPad
        case "Double":
            return .decimalPad
        case "Bool":
            return .asciiCapable
        default:
            return .default
        }
    }

    private func showSaveError(_ message: String) {
        errorMessage = message
        showErrorAlert = true
    }
}

private enum PreferenceEditorMode {
    case create
    case edit
}
