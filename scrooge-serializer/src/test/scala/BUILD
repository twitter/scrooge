junit_tests(
    sources = ["**/*.scala"],
    compiler_option_sets = {"fatal_warnings"},
    dependencies = [
        "3rdparty/jvm/junit",
        "3rdparty/jvm/org/apache/thrift:libthrift",
        "3rdparty/jvm/org/scalatest",
        "3rdparty/jvm/org/slf4j:slf4j-nop",
        "scrooge/scrooge-core/src/main/scala",
        "scrooge/scrooge-serializer",
        "scrooge/scrooge-serializer/src/test/thrift:thrift-scala",
        "util/util-app/src/main/scala",
    ],
)
