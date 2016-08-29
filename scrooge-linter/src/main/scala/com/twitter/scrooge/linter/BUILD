scala_library(
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'scrooge-linter',
    repo = artifactory,
  ),
  name='scala',
  dependencies=[
    '3rdparty/jvm/com/github/scopt',
    'scrooge/scrooge-generator',
    'util/util-logging',
    '3rdparty/jvm/org/scala-lang/modules:scala-parser-combinators',
  ],
  fatal_warnings=True,
  sources=globs('*.scala'),
)

jvm_binary(
  name='bin',
  basename='scrooge-linter',
  main='com.twitter.scrooge.linter.Main',
  dependencies=[':scala']
)
