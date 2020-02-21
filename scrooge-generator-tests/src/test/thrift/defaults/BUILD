java_thrift_library(
    name = "thrift-scala",
    sources = ["**/*.thrift"],
    compiler = "scrooge",
    language = "scala",
)

java_thrift_library(
    name = "thrift-java",
    sources = ["**/*.thrift"],
    compiler = "scrooge",
    language = "java",
)

java_thrift_library(
    name = "thrift-android",
    sources = ["**/*.thrift"],
    compiler = "scrooge",
    default_java_namespace = "apache_java_thrift",
    language = "android",
    dependencies = [
        "3rdparty/jvm/org/apache/thrift:libthrift",
    ],
)
