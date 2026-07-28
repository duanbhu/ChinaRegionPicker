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
            pkgConfig: "sqlite3",
            providers: [
                .apt(["libsqlite3-dev"]),
                .brew(["sqlite3"])
            ]
        ),
        .target(
            name: "ChinaRegionPicker",
            dependencies: ["CSQLite"],
            resources: [.copy("Resources")],
            linkerSettings: [.linkedLibrary("sqlite3")]
        ),
        .testTarget(
            name: "ChinaRegionPickerTests",
            dependencies: ["ChinaRegionPicker"]
        )
    ]
)
