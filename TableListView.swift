//
//  TableListView.swift
//  CapacitorPluginDataViewer
//
//  Created by cmd on 3/3/26.
//

import Foundation
import SwiftUI

struct TableListView: View {
    let dbUrl: URL
    @State private var tables: [String] = []
    let repository = DatabaseRepository.shared
    
    var body: some View {
        List(tables, id: \.self) { tableName in
            NavigationLink(destination: DataGridScreen(dbUrl: dbUrl, tableName: tableName)) {
                HStack {
                    Image(systemName: "tablecells")
                        .foregroundColor(.green)
                    Text(tableName)
                        .font(.body)
                }
            }
        }
        .navigationTitle(dbUrl.lastPathComponent)
        .onAppear {
            tables = repository.getTables(from: dbUrl)
        }
    }
}
