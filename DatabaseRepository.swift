//
//  DatabaseRepository.swift
//  CapacitorPluginDataViewer
//
//  Created by cmd on 3/3/26.
//

import Foundation
import SQLite3

class DatabaseRepository {
    static let shared = DatabaseRepository()
    
    private init() {}
    
    func getDatabaseFiles() -> [URL] {
        let fileManager = FileManager.default
        
        let appSupportDir = fileManager.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
        let documentsDir = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first!
        let directories = [appSupportDir, documentsDir]
        var dbFiles: [URL] = []
                
        for rootDir in directories {
            guard let enumerator = fileManager.enumerator(at: rootDir,
                                                        includingPropertiesForKeys: [.isRegularFileKey],
                                                        options: [.skipsHiddenFiles]) else { continue }
            
            for case let fileURL as URL in enumerator {
                do {
                    let resourceValues = try fileURL.resourceValues(forKeys: [.isRegularFileKey])
                    guard resourceValues.isRegularFile == true else { continue }
                    
                    let name = fileURL.lastPathComponent
                    let ext = fileURL.pathExtension.lowercased()
                    
                    let isDbFile = (ext == "db" || ext == "sqlite" || ext == "")
                    let isNotTempFile = !name.hasSuffix("-wal") &&
                                        !name.hasSuffix("-shm") &&
                                        !name.hasSuffix("-journal")
                    
                    if isDbFile && isNotTempFile {
                        dbFiles.append(fileURL)
                    }
                } catch {
                    print("Lỗi khi kiểm tra file: \(error)")
                }
            }
        }
        return dbFiles
    }
    
    func getTables(from dbUrl: URL) -> [String] {
        var db: OpaquePointer?
        var tables: [String] = []
        
        if sqlite3_open_v2(dbUrl.path, &db, SQLITE_OPEN_READONLY, nil) == SQLITE_OK {
            let query = "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'pc_stat_%';"
            var statement: OpaquePointer?
            
            if sqlite3_prepare_v2(db, query, -1, &statement, nil) == SQLITE_OK {
                while sqlite3_step(statement) == SQLITE_ROW {
                    if let cString = sqlite3_column_text(statement, 0) {
                        tables.append(String(cString: cString))
                    }
                }
            }
            sqlite3_finalize(statement)
        } else {
            print("Could not open database in read-only mode")
        }
        
        sqlite3_close(db)
        return tables.sorted()
    }
    
    func getTableData(from dbUrl: URL, tableName: String, limit: Int, offset: Int) -> (columns: [String], rows: [[String]]) {
        var db: OpaquePointer?
        var columns: [String] = []
        var rows: [[String]] = []
        
        if sqlite3_open_v2(dbUrl.path, &db, SQLITE_OPEN_READONLY, nil) == SQLITE_OK {
            // Sử dụng LIMIT và OFFSET để phân trang
            let query = "SELECT * FROM \(tableName) LIMIT \(limit) OFFSET \(offset);"
            var statement: OpaquePointer?
            
            if sqlite3_prepare_v2(db, query, -1, &statement, nil) == SQLITE_OK {
                let columnCount = sqlite3_column_count(statement)
                
                for i in 0..<columnCount {
                    let colName = String(cString: sqlite3_column_name(statement, i))
                    columns.append(colName)
                }
                
                while sqlite3_step(statement) == SQLITE_ROW {
                    var row: [String] = []
                    for i in 0..<columnCount {
                        if let cString = sqlite3_column_text(statement, i) {
                            row.append(String(cString: cString))
                        } else {
                            row.append("NULL")
                        }
                    }
                    rows.append(row)
                }
            }
            sqlite3_finalize(statement)
        }
        sqlite3_close(db)
        return (columns, rows)
    }
}
