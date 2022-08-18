junit_tests(
    name = "scala",
    sources = ["**/*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/junit",
        "3rdparty/jvm/org/mockito:mockito-all",
        "3rdparty/jvm/org/scalatest",
        "3rdparty/jvm/org/scalatestplus:junit",
        "3rdparty/jvm/org/scalatestplus:mockito-1-10",
        "scrooge/scrooge-generator/src/main/scala",
        "scrooge/scrooge-linter/src/main/scala/com/twitter/scrooge/linter",
    ],
)
