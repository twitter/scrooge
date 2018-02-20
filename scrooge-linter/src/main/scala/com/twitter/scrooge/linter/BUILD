scala_library(
    sources = globs("*.scala"),
    fatal_warnings = True,
    provides = scala_artifact(
        org = "com.twitter",
        name = "scrooge-linter",
        repo = artifactory,
    ),
    dependencies = [
        "3rdparty/jvm/com/github/scopt",
        "3rdparty/jvm/org/scala-lang/modules:scala-parser-combinators",
        "scrooge/scrooge-generator",
        "util/util-logging",
    ],
    exports = [
        "3rdparty/jvm/com/github/scopt",
    ],
)
