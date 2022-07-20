scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    provides = scala_artifact(
        org = "com.twitter",
        name = "scrooge-linter",
        repo = artifactory,
    ),
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/com/github/scopt",
        "3rdparty/jvm/org/scala-lang/modules:scala-parser-combinators",
        "scrooge/scrooge-generator",
        "util/util-app",
    ],
    exports = [
        "3rdparty/jvm/com/github/scopt",
    ],
)
