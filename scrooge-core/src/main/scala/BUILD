scala_library(
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'scrooge-core',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/org/apache/thrift:libthrift-0.5.0',
    'util/util-core/src/main/scala:scala',
  ],
  exports=[
    'util/util-core/src/main/scala:scala',
  ],
  sources=rglobs('*.scala'),
  strict_deps=True,
)
