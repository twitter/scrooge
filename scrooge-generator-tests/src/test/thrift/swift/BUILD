create_thrift_libraries(
    base_name = "swift",
    sources = ["*.thrift"],
    tags = ["bazel-compatible"],
    generate_languages = [
        "scala",
    ],
)

resources(
    name = "idl-resources",
    sources = ["*.thrift"],
)
