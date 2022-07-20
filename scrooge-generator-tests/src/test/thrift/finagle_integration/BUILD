create_thrift_libraries(
    base_name = "thrift",
    sources = ["**/*.thrift"],
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
