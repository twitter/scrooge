java_thrift_library(
    name = "thrift-scala",
    sources = rglobs("*.thrift"),
    compiler = "scrooge",
    default_java_namespace = "bar",
    language = "scala",
)

java_thrift_library(
    name = "thrift-android",
    sources = rglobs("*.thrift"),
    compiler = "scrooge",
    default_java_namespace = "android_bar",
    language = "android",
    namespace_map = {
        "foo": "android_foo",
        "bar": "android_bar",
        "com.fake": "com.android_fake",
    },
    dependencies = [
        "3rdparty/jvm/org/apache/thrift:libthrift",
    ],
)
