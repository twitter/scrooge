scala_library(
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'scrooge-linter',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/github/scopt',
    '3rdparty/jvm/org/scala-lang/modules:scala-parser-combinators',
    'scrooge/scrooge-generator',
    'util/util-logging',
  ],
  fatal_warnings=True,
  sources=globs('*.scala'),
  exports=[
    '3rdparty/jvm/com/github/scopt:scopt',
  ],
)
