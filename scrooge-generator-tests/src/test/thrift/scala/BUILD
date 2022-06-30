create_thrift_libraries(
    base_name = "thrift",
    sources = ["**/*.thrift"],
    tags = ["bazel-compatible"],
    generate_languages = [
        "java",
        "scala",
        "typescript",
    ],
)

resources(
    name = "resources",
    sources = [
        "*.thrift",
        "inheritance/*.thrift",
    ],
    tags = ["bazel-compatible"],
)
