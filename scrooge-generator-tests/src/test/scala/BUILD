# Scrooge language implementations that live outside of the scrooge directory
# benefit from being able to depend on GoldFileTest.
# In order to make GoldFileTest available to other pants targets as a dependency
# it needs to belong to its own 'java_library' target, ideally without additional
# dependencies like all the unit tests below (depending on all those extra tests would
# cause them to be run twice anyway). So here we define a small pants target for just
# GoldFileTest and the dependencies it needs and expose it as a 'java_library'.
#
# Below, in the junit_tests target we depend on this 'lib' target, but make sure to exclude
# these sources, as we want to depend on the pants target that 'owns' the sources and not the
# source files directly. Depending directly on the source files breaks a pants rule that
# no 2 targets can 'own' the same source files.
#
# NOTE: In the future, we should consider splitting GoldFileTest into its own jvm package
# with its own BUILD file which would simplify this.

LIB_SOURCES = [
    "com/twitter/scrooge/goldfile/GoldFileTest.scala",
    "com/twitter/scrooge/testutil/Utils.scala",
    "com/twitter/scrooge/testutil/TempDirectory.scala",
]

java_library(
    name = "lib",
    sources = LIB_SOURCES,
    dependencies = [
        "3rdparty/jvm/junit",
        "3rdparty/jvm/org/scalatest",
        "scrooge/scrooge-core",
        "scrooge/scrooge-generator",
    ],
)

junit_tests(

    # see comments above, we need to avoid 'owning' the LIB_SOURCES files
    # and instead 'depend' on them
    sources = ["**/*.scala"] + exclude_globs(LIB_SOURCES),
    compiler_option_sets = ["fatal_warnings"],
    dependencies = [
        ":lib",
        "3rdparty/jvm/cglib",
        "3rdparty/jvm/com/github/spullara/mustache/java",
        "3rdparty/jvm/com/google/guava",
        "3rdparty/jvm/junit",
        "3rdparty/jvm/org/apache/thrift:libthrift",
        "3rdparty/jvm/org/hamcrest:hamcrest-core",
        "3rdparty/jvm/org/jmock",
        "3rdparty/jvm/org/jmock:jmock-legacy",
        "3rdparty/jvm/org/mockito:mockito-all",
        "3rdparty/jvm/org/objenesis",
        "3rdparty/jvm/org/ow2/asm",
        "3rdparty/jvm/org/scala-lang/modules:scala-parser-combinators",
        "3rdparty/jvm/org/scalatest",
        "3rdparty/jvm/org/scalatestplus:jmock",
        "3rdparty/jvm/org/scalatestplus:junit",
        "3rdparty/jvm/org/scalatestplus:mockito-1-10",
        "3rdparty/jvm/org/slf4j:slf4j-nop",
        "finagle/finagle-core/src/main",
        "finagle/finagle-thrift/src/main/java",
        "finagle/finagle-thrift/src/main/scala",
        "finagle/finagle-thriftmux",
        "scrooge/scrooge-core",
        "scrooge/scrooge-generator",
        "scrooge/scrooge-generator-tests/src/test/resources",
        "scrooge/scrooge-generator-tests/src/test/thrift",
        "scrooge/scrooge-generator-tests/src/test/thrift:constant-sets-thrift-bazel-workaround-android",
        "scrooge/scrooge-generator-tests/src/test/thrift:constant-sets-thrift-scala",
        "scrooge/scrooge-generator-tests/src/test/thrift/airport:thrift-bazel-workaround-android",
        "scrooge/scrooge-generator-tests/src/test/thrift/airport:thrift-namespace-bazel-workaround-android",
        "scrooge/scrooge-generator-tests/src/test/thrift/airport:thrift-namespace-scala",
        "scrooge/scrooge-generator-tests/src/test/thrift/airport:thrift-scala",
        "scrooge/scrooge-generator-tests/src/test/thrift/android_integration:thrift-android",
        "scrooge/scrooge-generator-tests/src/test/thrift/defaults:thrift-bazel-workaround-android",
        "scrooge/scrooge-generator-tests/src/test/thrift/defaults:thrift-java",
        "scrooge/scrooge-generator-tests/src/test/thrift/defaults:thrift-scala",
        "scrooge/scrooge-generator-tests/src/test/thrift/finagle_integration:thrift-scala",
        "scrooge/scrooge-generator-tests/src/test/thrift/integration:thrift-bazel-workaround-1-scala",
        "scrooge/scrooge-generator-tests/src/test/thrift/integration:thrift-bazel-workaround-android",
        "scrooge/scrooge-generator-tests/src/test/thrift/integration:thrift-java",
        "scrooge/scrooge-generator-tests/src/test/thrift/namespace:thrift-bazel-workaround-android",
        "scrooge/scrooge-generator-tests/src/test/thrift/namespace:thrift-scala",
        "scrooge/scrooge-generator-tests/src/test/thrift/relative:thrift-bazel-workaround-android",
        "scrooge/scrooge-generator-tests/src/test/thrift/relative:thrift-scala",
        "scrooge/scrooge-generator-tests/src/test/thrift/scala:thrift-java",
        "scrooge/scrooge-generator-tests/src/test/thrift/scala:thrift-scala",
        "scrooge/scrooge-generator-tests/src/test/thrift/standalone:thrift-all-scala",
        "scrooge/scrooge-generator-tests/src/test/thrift/standalone:thrift-bazel-workaround-android",
        "scrooge/scrooge-generator-tests/src/test/thrift/standalone:thrift-java",
        "util/util-core:scala",
    ],
)
