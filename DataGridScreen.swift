//
//  DataGridScreen.swift
//  CapacitorPluginDataViewer
//
//  Created by cmd on 3/3/26.
//

import Foundation
import SwiftUI

struct DataGridScreen: View {
    let dbUrl: URL
    let tableName: String
    
    @State private var columns: [String] = []
    @State private var rows: [[String]] = []
    @State private var offset: Int = 0
    @State private var isLoading = false
    @State private var canLoadMore = true
    
    @State private var showActionSheet = false
    @State private var selectedCellText = ""
    
    private let pageSize = 50
    private let repository = DatabaseRepository.shared
    private let columnWidth: CGFloat = 120

    var body: some View {
        VStack(spacing: 0) {
            if columns.isEmpty && isLoading {
                ProgressView("Loading...")
            } else {
                ScrollView(.horizontal) {
                    ScrollView(.vertical) {
                        LazyVStack(alignment: .leading, spacing: 0, pinnedViews: [.sectionHeaders]) {
                            Section(header: headerView) {
                                ForEach(0..<rows.count, id: \.self) { index in
                                    rowView(rows[index], index: index)
                                        .onAppear {
                                            // Nếu cuộn đến hàng cuối cùng, tự động load trang tiếp theo
                                            if index == rows.count - 1 && canLoadMore {
                                                loadMoreData()
                                            }
                                        }
                                    Divider()
                                }
                                
                                if isLoading {
                                    ProgressView()
                                        .frame(maxWidth: .infinity, minHeight: 44)
                                }
                            }
                        }
                    }
                    .frame(width: CGFloat(columns.count) * columnWidth)
                }
            }
        }
        .navigationTitle(tableName)
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
            if rows.isEmpty { loadMoreData() }
        }
    }

    private func loadMoreData() {
        guard !isLoading else { return }
        isLoading = true
        
        // Chạy trong background để không lag UI
        DispatchQueue.global(qos: .userInitiated).async {
            let newData = repository.getTableData(
                from: dbUrl,
                tableName: tableName,
                limit: pageSize,
                offset: offset
            )
            
            DispatchQueue.main.async {
                if self.columns.isEmpty {
                    self.columns = newData.columns
                }
                
                if newData.rows.isEmpty {
                    self.canLoadMore = false
                } else {
                    self.rows.append(contentsOf: newData.rows)
                    self.offset += pageSize
                }
                self.isLoading = false
            }
        }
    }

    // Header và Row View giữ nguyên như phần trước...
    var headerView: some View {
        HStack(spacing: 0) {
            ForEach(columns, id: \.self) { col in
                Text(col)
                    .font(.caption.bold())
                    .frame(width: columnWidth, height: 40, alignment: .leading)
                    .padding(.horizontal, 8)
                    .background(Color(.systemGray6))
                    .border(Color(.systemGray4), width: 0.5)
            }
        }
    }

    func rowView(_ row: [String], index: Int) -> some View {
        HStack(spacing: 0) {
            ForEach(0..<row.count, id: \.self) { colIndex in
                Text(row[colIndex])
                    .font(.system(size: 12))
                    .frame(width: columnWidth, height: 35, alignment: .leading)
                    .padding(.horizontal, 8)
                    .lineLimit(1)
                    .background(index % 2 == 0 ? Color(.systemBackground) : Color(.secondarySystemBackground))
                    .onTapGesture {
                        self.selectedCellText = row[colIndex]
                        self.showActionSheet = true
                    }
            }
        }
    }
}
