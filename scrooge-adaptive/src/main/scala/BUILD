scala_library(
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'scrooge-adaptive',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/asm',
    '3rdparty/jvm/asm:asm-commons',
    '3rdparty/jvm/asm:asm-util',
    '3rdparty/jvm/org/apache/thrift:libthrift-0.5.0',
    'scrooge/scrooge-core',
    'scrooge/scrooge-serializer',
    'util/util-codec/src/main/scala:scala',
    'util/util-logging/src/main/scala:scala',
  ],
  exports=[
    '3rdparty/jvm/asm',
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala'),
)

