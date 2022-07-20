target(
    dependencies = [
        "scrooge/scrooge-generator/src/main/scala",
    ],
)

jvm_binary(
    name = "bin",
    main = "com.twitter.scrooge.Main",
    platform = "java8",
    tags = ["bazel-compatible"],
    dependencies = [
        "scrooge/scrooge-generator/src/main/scala",
    ],
)
