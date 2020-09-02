scala_library(
    sources = ["**/*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    dependencies = [
        "3rdparty/jvm/org/apache/thrift:libthrift",
        "3rdparty/jvm/org/openjdk/jmh:jmh-core",
        "scrooge/scrooge-adaptive",
        "scrooge/scrooge-adaptive/src/test/scala/com/twitter/scrooge/adapt/testutil",
        "scrooge/scrooge-benchmark/src/main/thrift:thrift-scala",
        "scrooge/scrooge-core/src/main/scala",
        "scrooge/scrooge-serializer",
    ],
    exports = [
        "3rdparty/jvm/org/openjdk/jmh:jmh-core",
    ],
)

jvm_binary(
    name = "jmh",
    main = "org.openjdk.jmh.Main",
    dependencies = [
        ":scala",
        scoped(
            "3rdparty/jvm/org/slf4j:slf4j-nop",
            scope = "runtime",
        ),
    ],
)
