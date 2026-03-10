import Foundation
import SwiftUI

struct DataGridScreen: View {
    let dbUrl: URL
    let tableName: String
    
    @State private var rows: [[String]] = []
    @State private var offset: Int = 0
    @State private var isLoading = false
    @State private var canLoadMore = true
  
    @State private var allColumns: [String] = []
    @State private var visibleColumns = Set<String>()
    @State private var filters: [FilterCondition] = []
    @State private var showSettings = false
    
    @State private var showActionSheet = false
    @State private var selectedCellText = ""
    @State private var sortColumn: String?
    @State private var sortOrder: GridSortOrder = .none
    
    private let pageSize = 50
    private let repository = DatabaseRepository.shared
    private let columnWidth: CGFloat = 150

    var body: some View {
        VStack(spacing: 0) {
            if allColumns.isEmpty && isLoading {
                ProgressView("Loading...")
            } else {
                ScrollView(.horizontal) {
                    ScrollView(.vertical) {
                        LazyVStack(alignment: .leading, spacing: 0, pinnedViews: [.sectionHeaders]) {
                            Section(header: makeHeaderView()) { // Gọi hàm tạo Header
                                ForEach(rows.indices, id: \.self) { index in
                                    makeRowView(rows[index], index: index)
                                        .onAppear {
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
                    // Tính độ rộng dựa trên số cột đang hiển thị
                    .frame(width: CGFloat(visibleColumns.isEmpty ? allColumns.count : visibleColumns.count) * columnWidth)
                }
            }
        }
        .navigationTitle(tableName)
        .navigationBarItems(trailing: Button(action: { showSettings = true }) {
            Image(systemName: "line.3.horizontal.decrease.circle")
        })
        .sheet(isPresented: $showSettings, onDismiss: {
            refreshData()
        }) {
            // Đảm bảo bạn đã định nghĩa TableSettingsView hỗ trợ iOS 12 như mình đã hướng dẫn
            TableSettingsView(allColumns: allColumns, visibleColumns: $visibleColumns, filters: $filters)
        }
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

    // Chuyển thành hàm để build dễ hơn
    private func makeHeaderView() -> some View {
        HStack(spacing: 0) {
            let displayCols = allColumns.filter { visibleColumns.contains($0) }
            ForEach(displayCols, id: \.self) { col in
                HStack(spacing: 4) {
                    Text(col)
                        .font(.caption.bold())
                        .lineLimit(1)

                    if sortColumn == col {
                        Image(systemName: sortOrder == .asc ? "arrowtriangle.up.fill" : "arrowtriangle.down.fill")
                            .font(.system(size: 9))
                            .foregroundColor(.blue)
                    }
                }
                .frame(width: columnWidth, height: 40, alignment: .leading)
                .padding(.horizontal, 8)
                .background(Color(.systemGray6))
                .border(Color(.systemGray4), width: 0.5)
                .contentShape(Rectangle())
                .onTapGesture {
                    toggleSort(for: col)
                }
            }
        }
    }

    private func makeRowView(_ row: [String], index: Int) -> some View {
        // Lấy danh sách index của những cột được chọn
        let visibleIndices = allColumns.enumerated()
            .filter { visibleColumns.contains($1) }
            .map { $0.offset }
      
        return HStack(spacing: 0) {
            ForEach(visibleIndices, id: \.self) { colIndex in
                // Kiểm tra index an toàn tránh crash
                Text(row.indices.contains(colIndex) ? row[colIndex] : "")
                    .font(.system(size: 12))
                    .frame(width: columnWidth, height: 35, alignment: .leading)
                    .padding(.horizontal, 8)
                    .lineLimit(1)
                    .background(index % 2 == 0 ? Color(.systemBackground) : Color(.secondarySystemBackground))
                    .onTapGesture {
                        if row.indices.contains(colIndex) {
                            self.selectedCellText = row[colIndex]
                            self.showActionSheet = true
                        }
                    }
            }
        }
    }

    private func loadMoreData() {
        guard !isLoading else { return }
        isLoading = true
        
        DispatchQueue.global(qos: .userInitiated).async {
            let newData = repository.getTableData(
                from: dbUrl,
                tableName: tableName,
                limit: pageSize,
                offset: offset,
                filters: filters,
                sortColumn: sortOrder == .none ? nil : sortColumn,
                sortOrder: sortOrder.sqlValue
            )
            
            DispatchQueue.main.async {
                if self.allColumns.isEmpty {
                    self.allColumns = newData.columns
                    self.visibleColumns = Set(newData.columns)
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
  
    private func refreshData() {
        self.offset = 0
        self.rows = []
        self.canLoadMore = true
        loadMoreData()
    }

    private func toggleSort(for column: String) {
        if sortColumn != column {
            sortColumn = column
            sortOrder = .asc
        } else {
            switch sortOrder {
            case .none:
                sortOrder = .asc
            case .asc:
                sortOrder = .desc
            case .desc:
                sortOrder = .none
                sortColumn = nil
            }
        }
        refreshData()
    }
}

private enum GridSortOrder {
    case none
    case asc
    case desc

    var sqlValue: String? {
        switch self {
        case .none:
            return nil
        case .asc:
            return "ASC"
        case .desc:
            return "DESC"
        }
    }
}
