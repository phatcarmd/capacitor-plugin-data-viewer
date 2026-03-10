//
//  DataExplorerView.swift
//  CapacitorPluginDataViewer
//
//  Created by cmd on 3/3/26.
//

import Foundation
import SwiftUI
import UIKit

struct DataExplorerView: View {
    @State private var dbFiles: [URL] = []
    @State private var prefFiles: [URL] = []
    @State private var isSelectionMode = false
    @State private var selectedFiles = Set<URL>()
    @State private var showShareSheet = false
    
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
                        if isSelectionMode {
                            Button(action: { toggleSelection(for: file) }) {
                                FileRowView(
                                    fileName: file.lastPathComponent,
                                    icon: "tablecells.fill",
                                    iconColor: .blue,
                                    selectionState: selectedFiles.contains(file)
                                )
                            }
                            .buttonStyle(PlainButtonStyle())
                        } else {
                            NavigationLink(destination: TableListView(dbUrl: file)) {
                                FileRowView(fileName: file.lastPathComponent, icon: "tablecells.fill", iconColor: .blue)
                            }
                        }
                    }
                }
                
                // --- SHARED PREFERENCES ---
                Section(header: Label("Shared Preferences", systemImage: "gearshape.fill")) {
                    ForEach(prefFiles, id: \.self) { file in
                        if isSelectionMode {
                            Button(action: { toggleSelection(for: file) }) {
                                FileRowView(
                                    fileName: file.lastPathComponent,
                                    icon: "doc.text.fill",
                                    iconColor: .orange,
                                    selectionState: selectedFiles.contains(file)
                                )
                            }
                            .buttonStyle(PlainButtonStyle())
                        } else {
                            NavigationLink(destination: PreferenceDetailScreen(fileUrl: file)) {
                                FileRowView(fileName: file.lastPathComponent, icon: "doc.text.fill", iconColor: .orange)
                            }
                        }
                    }
                }
            }
            .listStyle(InsetGroupedListStyle())
            .navigationTitle("Data Explorer")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(isSelectionMode ? "Cancel" : "Select") {
                        if isSelectionMode {
                            selectedFiles.removeAll()
                        }
                        isSelectionMode.toggle()
                    }
                    .foregroundColor(.white)
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    if isSelectionMode {
                        Button("Share") {
                            showShareSheet = true
                        }
                        .disabled(selectedFiles.isEmpty)
                        .foregroundColor(.white)
                    }
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    if !isSelectionMode {
                        Button("Done") {
                            onDismiss()
                        }
                        .foregroundColor(.white)
                    }
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .sheet(isPresented: $showShareSheet) {
                ShareSheet(activityItems: selectedFiles.sorted { $0.lastPathComponent < $1.lastPathComponent })
            }
            .onAppear {
                dbFiles = repository.getDatabaseFiles()
                prefFiles = repository.getSharedPreferencesFiles()
            }
        }
        .accentColor(.white)
    }

    private func toggleSelection(for file: URL) {
        if selectedFiles.contains(file) {
            selectedFiles.remove(file)
        } else {
            selectedFiles.insert(file)
        }
    }
}

struct FileRowView: View {
    let fileName: String
    let icon: String
    let iconColor: Color
    var selectionState: Bool? = nil
    
    var body: some View {
        HStack {
            Image(systemName: icon)
                .foregroundColor(iconColor)
            Text(fileName)
                .font(.body)
                .lineLimit(2)
                .truncationMode(.middle)
                .foregroundColor(.primary)

            if let isSelected = selectionState {
                Spacer()
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .foregroundColor(isSelected ? .green : .gray)
            }
        }
        .padding(.vertical, 4)
    }
}

struct ShareSheet: UIViewControllerRepresentable {
    let activityItems: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
