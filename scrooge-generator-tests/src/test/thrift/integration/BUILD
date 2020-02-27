# todo: do we need --disable-strict support?
# https://github.com/pantsbuild/pants/blob/e966210d85a42e25232c569b95ce88f024a851c8/contrib/scrooge/src/python/pants/contrib/scrooge/tasks/scrooge_gen.py#L159
java_thrift_library(
    name = "thrift-scala",
    sources = ["**/*.thrift"],
    compiler = "scrooge",
    language = "scala",
    namespace_map = {
        "thrift.test": "com.twitter.scrooge.integration_scala",
    },
)

java_thrift_library(
    name = "thrift-android",
    sources = [
        "**/*.thrift",
        "scrooge/scrooge-generator-tests/src/test/thrift/android_integration/**/*.thrift",
    ],
    compiler = "scrooge",
    language = "android",
    namespace_map = {
        "thrift.test": "com.twitter.scrooge.integration_android",
        "com.twitter.scrooge.integration_scala": "com.twitter.scrooge.integration_android",
    },
    dependencies = [
        "3rdparty/jvm/org/apache/thrift:libthrift",
    ],
)

java_thrift_library(
    name = "thrift-java",
    sources = ["**/*.thrift"],
    compiler = "scrooge",
    language = "java",
    namespace_map = {
        "thrift.test": "com.twitter.scrooge.integration_apache",
        "com.twitter.scrooge.integration_scala": "com.twitter.scrooge.integration_apache",
    },
)
