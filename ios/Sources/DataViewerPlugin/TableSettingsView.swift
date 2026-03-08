//
//  TableSettingsView.swift
//  CapacitorPluginDataViewer
//
//  Created by cmd on 8/3/26.
//

import Foundation
import SwiftUI

struct TableSettingsView: View {
    // Thay cho @Environment(\.dismiss)
    @Environment(\.presentationMode) var presentationMode
    
    let allColumns: [String]
    @Binding var visibleColumns: Set<String>
    @Binding var filters: [FilterCondition]
    
    @State private var isColumnsExpanded = true
    @State private var isFiltersExpanded = true
    
    let operators = ["=", "LIKE", "IN", ">", "<", "!="]

    var body: some View {
        NavigationView {
            Form {
                // Section 1: Columns
                Section(header: headerToggle("Display Columns (\(visibleColumns.count))", isExpanded: $isColumnsExpanded)) {
                    if isColumnsExpanded {
                        ForEach(allColumns, id: \.self) { column in
                            Toggle(column, isOn: Binding(
                                get: { visibleColumns.contains(column) },
                                set: { isOn in
                                    if isOn { visibleColumns.insert(column) }
                                    else { visibleColumns.remove(column) }
                                }
                            ))
                        }
                    }
                }

                // Section 2: Filters
                Section(header: headerToggle("Data Filters (\(filters.count))", isExpanded: $isFiltersExpanded)) {
                    if isFiltersExpanded {
                        ForEach($filters) { $filter in
                            filterRow(filter: $filter) {
                                let deletingId = filter.id
                                filters.removeAll { $0.id == deletingId }
                            }
                        }
                        
                        Button(action: {
                            filters.append(FilterCondition(column: allColumns.first ?? ""))
                        }) {
                            Text("+ Add Filter").foregroundColor(.blue)
                        }
                    }
                }
            }
            .navigationBarTitle("Table Settings", displayMode: .inline)
            .navigationBarItems(trailing: Button("Apply") {
                presentationMode.wrappedValue.dismiss()
            }
            .foregroundColor(.white))
        }
    }

    // Hàm tạo Header có thể nhấn để đóng/mở (Thay cho DisclosureGroup)
    private func headerToggle(_ title: String, isExpanded: Binding<Bool>) -> some View {
        Button(action: { isExpanded.wrappedValue.toggle() }) {
            HStack {
                Text(title)
                Spacer()
                Image(systemName: isExpanded.wrappedValue ? "chevron.up" : "chevron.down")
            }
        }
    }

    // Row filter rendered from Binding to keep UI in sync after delete.
    private func filterRow(filter: Binding<FilterCondition>, onDelete: @escaping () -> Void) -> some View {
        VStack(alignment: .leading) {
            HStack {
                Picker("", selection: filter.column) {
                    ForEach(allColumns, id: \.self) { Text($0).tag($0) }
                }.labelsHidden()
                
                Picker("", selection: filter.op) {
                    ForEach(operators, id: \.self) { Text($0).tag($0) }
                }.labelsHidden()
                
                Spacer()
                
                // Thay cho Button(role: .destructive)
                Button(action: onDelete) {
                    Image(systemName: "trash").foregroundColor(.red)
                }
                .buttonStyle(BorderlessButtonStyle())
                .contentShape(Rectangle())
                .padding(.horizontal, 6)
            }
            TextField("Value...", text: filter.value)
                .textFieldStyle(RoundedBorderTextFieldStyle())
        }
    }
}
