scala_library(
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'scrooge-serializer',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/org/apache/thrift:libthrift-0.5.0',
    'util/util-app',
    'util/util-codec',
    'scrooge/scrooge-core'
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala'),
)
