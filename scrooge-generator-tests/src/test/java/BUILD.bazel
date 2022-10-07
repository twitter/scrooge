junit_tests(
    sources = ["**/*.java"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/junit",
        "3rdparty/jvm/org/apache/thrift:libthrift",
        "3rdparty/jvm/org/scalatestplus:junit",
        scoped(
            "3rdparty/jvm/org/slf4j:slf4j-nop",
            scope = "runtime",
        ),
        "finagle/finagle-core/src/main",
        "finagle/finagle-core/src/main/java",
        "finagle/finagle-thrift",
        "finagle/finagle-thriftmux",
        "scrooge/scrooge-core/src/main/scala",
        "scrooge/scrooge-generator-tests/src/test/thrift/finagle_integration:thrift-java",
        "util/util-core:scala",
    ],
)
