// swift-tools-version: 5.9
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "egk_demo_app_native",
    platforms: [
        .iOS(.v16)
    ],
    products: [
        .library(name: "egk-demo-app-native", targets: ["egk_demo_app_native"])
    ],
    dependencies: [
        .package(url: "https://github.com/gematik/ref-OpenHealthCardKit", from: "5.6.0"),
        .package(url: "https://github.com/1024jp/GzipSwift", from: "6.0.0"),
        .package(url: "https://github.com/yahoojapan/SwiftyXMLParser", from: "5.6.0")
    ],
    targets: [
        .target(
            name: "egk_demo_app_native",
            dependencies: [
                .product(name: "CardReaderProviderApi", package: "ref-OpenHealthCardKit"),
                .product(name: "HealthCardAccess", package: "ref-OpenHealthCardKit"),
                .product(name: "HealthCardControl", package: "ref-OpenHealthCardKit"),
                .product(name: "NFCCardReaderProvider", package: "ref-OpenHealthCardKit"),
                .product(name: "Gzip", package: "GzipSwift"),
                .product(name: "SwiftyXMLParser", package: "SwiftyXMLParser")
            ],
            resources: [
                .process("PrivacyInfo.xcprivacy"),
            ]
        )
    ]
)
