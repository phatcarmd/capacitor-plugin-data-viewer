//
//  DataExplorerView.swift
//  CapacitorPluginDataViewer
//
//  Created by cmd on 3/3/26.
//

import Foundation
import SwiftUI

struct DataExplorerView: View {
    @State private var dbFiles: [URL] = []
    @State private var prefFiles: [URL] = []
    
    let repository = DatabaseRepository.shared
    let onDismiss: () -> Void

    init(onDismiss: @escaping () -> Void) {
        self.onDismiss = onDismiss
        let appearance = UINavigationBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = UIColor.systemPurple
        appearance.titleTextAttributes = [.foregroundColor: UIColor.white]
        appearance.largeTitleTextAttributes = [.foregroundColor: UIColor.white]
        
        UINavigationBar.appearance().standardAppearance = appearance
        UINavigationBar.appearance().scrollEdgeAppearance = appearance
        UINavigationBar.appearance().compactAppearance = appearance
        UINavigationBar.appearance().tintColor = .white
    }

    var body: some View {
        NavigationView {
            List {
                // --- DATABASES ---
                Section(header: Label("Databases", systemImage: "list.bullet")) {
                    ForEach(dbFiles, id: \.self) { file in
                        NavigationLink(destination: TableListView(dbUrl: file)) {
                            FileRowView(fileName: file.lastPathComponent, icon: "tablecells.fill", iconColor: .blue)
                        }
                    }
                }
                
                // --- SHARED PREFERENCES ---
                Section(header: Label("Shared Preferences", systemImage: "gearshape.fill")) {
                    ForEach(prefFiles, id: \.self) { file in
                        NavigationLink(destination: PreferenceDetailScreen(fileUrl: file)) {
                            FileRowView(fileName: file.lastPathComponent, icon: "doc.text.fill", iconColor: .orange)
                        }
                    }
                }
            }
            .listStyle(InsetGroupedListStyle())
            .navigationTitle("Data Explorer")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        onDismiss()
                    }
                    .foregroundColor(.white)
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .onAppear {
                dbFiles = repository.getDatabaseFiles()
                prefFiles = repository.getSharedPreferencesFiles()
            }
        }
        .accentColor(.white)
    }
}

struct FileRowView: View {
    let fileName: String
    let icon: String
    let iconColor: Color
    
    var body: some View {
        HStack {
            Image(systemName: icon)
                .foregroundColor(iconColor)
            Text(fileName)
                .font(.body)
                .lineLimit(2)
                .truncationMode(.middle)
        }
        .padding(.vertical, 4)
    }
}
