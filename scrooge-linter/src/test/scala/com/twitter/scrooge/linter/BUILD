junit_tests(
    name = "scala",
    sources = ["**/*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    dependencies = [
        "3rdparty/jvm/junit",
        "3rdparty/jvm/org/mockito:mockito-all",
        "3rdparty/jvm/org/scalatest",
        "scrooge/scrooge-generator/src/main/scala",
        "scrooge/scrooge-linter/src/main/scala/com/twitter/scrooge/linter",
    ],
)
