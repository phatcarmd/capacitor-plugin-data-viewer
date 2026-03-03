//
//  DataExplorerView.swift
//  CapacitorPluginDataViewer
//
//  Created by cmd on 3/3/26.
//

import Foundation
import SwiftUI

struct DataExplorerView : View {
    @State private var files: [URL] = []
    let repository = DatabaseRepository.shared
    let onDismiss: () -> Void

    var body: some View {
        NavigationView {
            List(files, id: \.self) { file in
                NavigationLink(destination: TableListView(dbUrl: file)) {
                    HStack {
                        Image(systemName: "database.fill")
                            .foregroundColor(.blue)
                        VStack(alignment: .leading) {
                            Text(file.lastPathComponent)
                                .font(.headline)
                            Text(file.lastPathComponent) // Hoặc size/path ngắn gọn
                                .font(.caption2)
                                .foregroundColor(.gray)
                        }
                    }
                }
            }
            .navigationTitle("Data Explorer")
            .navigationBarItems(trailing: Button("Done") {
                onDismiss()
            })
            .onAppear {
                files = repository.getDatabaseFiles()
            }
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }
}
