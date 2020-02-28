resources(
    sources = [
        "!BUILD*",
        "**/*",
    ],
)

java_thrift_library(
    name = "constant-sets-thrift-scala",
    sources = ["constant_sets.thrift"],
    compiler = "scrooge",
    language = "scala",
)

java_thrift_library(
    name = "constant-sets-thrift-android",
    sources = ["constant_sets.thrift"],
    compiler = "scrooge",
    default_java_namespace = "android_thrift_default_namespace",
    language = "android",
    dependencies = [
        "3rdparty/jvm/org/apache/thrift:libthrift",
    ],
)
