create_thrift_libraries(
    base_name = "thrift",
    sources = ["**/*.thrift"],
    tags = ["bazel-compatible"],
    generate_languages = [
        "java",
        "scala",
    ],
)

resources(
    name = "resources",
    sources = ["*.thrift"],
)
