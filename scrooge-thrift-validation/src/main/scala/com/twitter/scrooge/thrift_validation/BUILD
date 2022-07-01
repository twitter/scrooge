scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    provides = scala_artifact(
        org = "com.twitter",
        name = "scrooge-core-thrift_validation",
        repo = artifactory,
    ),
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/org/hibernate/validator:hibernate-validator",
        "util/util-validator-constraints/src/main/java/com/twitter/util/validation/constraints",
    ],
)
