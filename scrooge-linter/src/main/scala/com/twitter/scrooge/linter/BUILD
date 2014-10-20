scala_library(
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'scrooge-linter',
    repo = artifactory,
  ),
  name='scala',
  dependencies=[
    '3rdparty:libthrift-0.5.0',
    'scrooge/scrooge-generator',
    'util/util-logging',
  ],
  sources=globs('*.scala'),
  resources=[
    'scrooge/scrooge-linter/src/main/resources'
  ],
)

jvm_binary(
  name='bin',
  basename='scrooge-linter',
  main='com.twitter.scrooge.linter.Main',
  dependencies=[':scala']
)
