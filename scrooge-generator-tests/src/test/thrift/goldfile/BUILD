create_thrift_libraries(
    base_name = "thrift",
    sources = ["*.thrift"],
    compiler_args = [
        "--finagle",
        "--java-passthrough",
        "--gen-adapt",
    ],
    platform = "java8",
    tags = ["bazel-compatible"],
    generate_languages = [
        "java",
        "scala",
    ],
)

resources(
    name = "resources",
    sources = ["*.thrift"],
    tags = ["bazel-compatible"],
)
