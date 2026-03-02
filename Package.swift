// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorPluginDataViewer",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "CapacitorPluginDataViewer",
            targets: ["DataViewerPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.0.0")
    ],
    targets: [
        .target(
            name: "DataViewerPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/DataViewerPlugin"),
        .testTarget(
            name: "DataViewerPluginTests",
            dependencies: ["DataViewerPlugin"],
            path: "ios/Tests/DataViewerPluginTests")
    ]
)