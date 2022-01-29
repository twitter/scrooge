resources(
    sources = ["*"],
    dependencies = [
        "scrooge/scrooge-generator-tests/src/test/thrift/airport:resources",
        "scrooge/scrooge-generator-tests/src/test/thrift/android_integration:resources",
        "scrooge/scrooge-generator-tests/src/test/thrift/defaults:resources",
        "scrooge/scrooge-generator-tests/src/test/thrift/finagle_integration:resources",
        "scrooge/scrooge-generator-tests/src/test/thrift/integration:resources",
        "scrooge/scrooge-generator-tests/src/test/thrift/namespace:resources",
        "scrooge/scrooge-generator-tests/src/test/thrift/relative:resources",
        "scrooge/scrooge-generator-tests/src/test/thrift/scala:resources",
        "scrooge/scrooge-generator-tests/src/test/thrift/standalone:resources",
    ],
)

java_thrift_library(
    name = "constant-sets-thrift-scala",
    sources = ["constant_sets.thrift"],
    compiler = "scrooge",
    language = "scala",
    tags = ["bazel-compatible"],
)

java_thrift_library(
    name = "constant-sets-thrift-bazel-workaround-android",
    sources = ["constant_sets.thrift"],
    compiler = "scrooge",
    default_java_namespace = "android_thrift_default_namespace",
    language = "android",
    dependencies = [
        "3rdparty/jvm/org/apache/thrift:libthrift",
    ],
)
