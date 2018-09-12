junit_tests(
    sources = rglobs("*.java"),
    compiler_option_sets = {"fatal_warnings"},
    dependencies = [
        "3rdparty/jvm/junit",
        "3rdparty/jvm/org/apache/thrift:libthrift",
        "finagle/finagle-core/src/main/java",
        "finagle/finagle-core/src/main/scala",
        "finagle/finagle-thrift",
        "finagle/finagle-thriftmux",
        "scrooge/scrooge-core/src/main/scala",
        "scrooge/scrooge-generator-tests/src/test/thrift/finagle_integration:thrift-java",
        "util/util-core/src/main/scala",
    ],
)
