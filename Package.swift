// swift-tools-version: 5.9

import PackageDescription

let package = Package(
    name: "ChinaRegionPicker",
    platforms: [
        .iOS(.v13),
        .macOS(.v12)
    ],
    products: [
        .library(name: "ChinaRegionPicker", targets: ["ChinaRegionPicker"])
    ],
    targets: [
        .systemLibrary(
            name: "CSQLite",
            path: "Swift/Sources/CSQLite",
            pkgConfig: "sqlite3",
            providers: [
                .apt(["libsqlite3-dev"]),
                .brew(["sqlite3"])
            ]
        ),
        .target(
            name: "ChinaRegionPicker",
            dependencies: ["CSQLite"],
            path: "Swift/Sources/ChinaRegionPicker",
            resources: [.copy("Resources")],
            linkerSettings: [.linkedLibrary("sqlite3")]
        ),
        .testTarget(
            name: "ChinaRegionPickerTests",
            dependencies: ["ChinaRegionPicker"],
            path: "Swift/Tests/ChinaRegionPickerTests"
        )
    ]
)
