java_thrift_library(
    name = "thrift-all-scala",
    sources = ["**/*.thrift"],
    compiler = "scrooge",
    language = "scala",
    platform = "java8",
    tags = ["bazel-compatible"],
)

java_thrift_library(
    name = "thrift-bazel-workaround-android",
    sources = [
        "constructorRequired.thrift",
        "enumSet.thrift",
        "exception_fields.thrift",
    ],
    compiler = "scrooge",
    default_java_namespace = "android_thrift_default_namespace",
    language = "android",
    platform = "java8",
    tags = ["bazel-compatible"],
)

java_thrift_library(
    name = "thrift-java",
    sources = [
        "constructorRequired.thrift",
        "enumSet.thrift",
        "exception_fields.thrift",
        "passthrough.thrift",
        "validations.thrift",
    ],
    compiler = "scrooge",
    compiler_args = ["--java-passthrough"],
    default_java_namespace = "apache_java_thrift",
    language = "java",
    namespace_map = {
        "thrift.test": "thrift.apache_java_test",
    },
    platform = "java8",
    tags = ["bazel-compatible"],
)

resources(
    name = "resources",
    sources = ["*.thrift"],
    tags = ["bazel-compatible"],
)
