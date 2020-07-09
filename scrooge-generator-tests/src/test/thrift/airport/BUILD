java_thrift_library(
    name = "thrift-scala",
    sources = ["**/*.thrift"],
    compiler = "scrooge",
    language = "scala",
)

java_thrift_library(
    # Renamed longer to assist bazel migration
    name = "thrift-bazel-workaround-android",
    sources = ["**/*.thrift"],
    compiler = "scrooge",
    language = "android",
    dependencies = [
        "3rdparty/jvm/org/apache/thrift:libthrift",
    ],
)

# namespaced and non-Finagle
java_thrift_library(
    name = "thrift-namespace-scala",
    sources = ["**/*.thrift"],
    compiler = "scrooge",
    language = "scala",
    namespace_map = {
        "thrift.test": "vanilla.test",
        "thrift.test1": "vanilla.test1",
        "thrift.test2": "vanilla.test2",
    },
)

java_thrift_library(
    # Renamed longer to assist bazel migration
    name = "thrift-namespace-bazel-workaround-android",
    sources = ["**/*.thrift"],
    compiler = "scrooge",
    language = "android",
    namespace_map = {
        "androidthrift.test": "vanilla_android.test",
        "androidthrift.test1": "vanilla_android.test1",
        "androidthrift.test2": "vanilla_android.test2",
    },
    dependencies = [
        "3rdparty/jvm/org/apache/thrift:libthrift",
    ],
)
