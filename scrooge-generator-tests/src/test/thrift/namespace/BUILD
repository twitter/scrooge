java_thrift_library(
    name = "thrift-scala",
    sources = ["**/*.thrift"],
    compiler = "scrooge",
    default_java_namespace = "bar",
    language = "scala",
    tags = ["bazel-compatible"],
)

java_thrift_library(
    # Renamed longer to assist bazel migration
    name = "thrift-bazel-workaround-android",
    sources = ["**/*.thrift"],
    compiler = "scrooge",
    default_java_namespace = "android_bar",
    language = "android",
    namespace_map = {
        "foo": "android_foo",
        "bar": "android_bar",
        "com.fake": "com.android_fake",
    },
    tags = ["bazel-compatible"],
)

resources(
    name = "resources",
    sources = ["*.thrift"],
    tags = ["bazel-compatible"],
)
