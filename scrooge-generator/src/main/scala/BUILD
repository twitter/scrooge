scala_library(
    sources = ["**/*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    provides = scala_artifact(
        org = "com.twitter",
        name = "scrooge-generator",
        repo = artifactory,
    ),
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/com/github/scopt",
        "3rdparty/jvm/com/github/spullara/mustache/java",
        "3rdparty/jvm/com/google/code/findbugs:jsr305",
        "3rdparty/jvm/com/google/guava",
        "3rdparty/jvm/commons-cli",
        "3rdparty/jvm/org/codehaus/plexus:plexus-utils",
        "3rdparty/jvm/org/scala-lang:scala-library",
        "3rdparty/jvm/org/scala-lang/modules:scala-parser-combinators",
        "scrooge/scrooge-generator/src/main/resources",
        "scrooge/scrooge-thrift-validation/src/main/scala/com/twitter/scrooge/thrift_validation",
    ],
    exports = [
        "3rdparty/jvm/com/github/scopt",
        "3rdparty/jvm/org/scala-lang/modules:scala-parser-combinators",
    ],
)
