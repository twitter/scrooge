// swift-tools-version:5.5

import PackageDescription

let package = Package(
    name: "ThriftTypeEncoding",
    products: [
        // Products define the executables and libraries produced by a package, and make them visible to other packages.
        .library(
            name: "ThriftTypeEncoding",
            targets: ["ThriftTypeEncoding"]),
    ],
    dependencies: [
        // Dependencies declare other packages that this package depends on.
        .package(name: "TwitterApacheThrift", url: "https://github.com/twitter/ios-twitter-apache-thrift", .upToNextMajor(from: "1.0.0"))
    ],
    targets: [
        // Targets are the basic building blocks of a package. A target can define a module or a test suite.
        // Targets can depend on other targets in this package, and on products in packages which this package depends on.
        .target(
            name: "ThriftTypeEncoding",
            dependencies: ["TwitterApacheThrift"]),
    ]
)
