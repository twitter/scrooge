create_thrift_libraries(
    base_name = "thrift",
    sources = ["**/*.thrift"],
    generate_languages = [
        "java",
        "scala",
        "scala-adaptive",
    ],
)

java_thrift_library(
    name = "thrift-bazel-workaround-android",
    sources = ["**/*.thrift"],
    compiler = "scrooge",
    default_java_namespace = "apache_java_thrift",
    language = "android",
    dependencies = [
        "3rdparty/jvm/org/apache/thrift:libthrift",
    ],
)
