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
    
    private let repository = DatabaseRepository.shared
    private let columnWidth: CGFloat = 150

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
        .actionSheet(isPresented: $showActionSheet) {
            ActionSheet(
                title: Text("Cell Options"),
                message: Text(selectedCellText),
                buttons: [
                    .default(Text("Copy")) {
                        UIPasteboard.general.string = selectedCellText
                    },
                    .cancel(Text("Close"))
                ]
            )
        }
        .onAppear {
            let data = repository.getPreferencesData(from: fileUrl)
            self.rows = data.rows
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
                    self.showActionSheet = true
                }
            }
        }
    }
}
