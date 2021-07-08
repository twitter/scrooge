jvm_binary(
    name = "bin",
    main = "com.twitter.scrooge.linter.Main",
    tags = ["bazel-compatible"],
    dependencies = [
        "scrooge/scrooge-linter/src/main/scala/com/twitter/scrooge/linter",
    ],
)

jvm_app(
    name = "app",
    basename = "scrooge-linter-package",
    binary = ":bin",
    tags = ["bazel-compatible"],
)
