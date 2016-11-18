scala_library(
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'scrooge-linter',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/github/scopt',
    'scrooge/scrooge-generator',
    'util/util-logging',
    '3rdparty/jvm/org/scala-lang/modules:scala-parser-combinators',
  ],
  fatal_warnings=True,
  sources=globs('*.scala'),
)
