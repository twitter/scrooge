junit_tests(
    sources = ["**/*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/junit",
        "3rdparty/jvm/org/apache/thrift:libthrift",
        "3rdparty/jvm/org/scalatest",
        "3rdparty/jvm/org/scalatestplus:junit",
        scoped(
            "3rdparty/jvm/org/slf4j:slf4j-nop",
            scope = "runtime",
        ),
        "scrooge/scrooge-core/src/main/scala",
        "scrooge/scrooge-serializer",
        "scrooge/scrooge-serializer/src/test/thrift:thrift-scala",
        "util/util-app/src/main/scala",
    ],
)
