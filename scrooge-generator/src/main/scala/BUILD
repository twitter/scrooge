scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'scrooge-generator',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty:scala-library',
    '3rdparty:commons-cli',
    '3rdparty:guava',
    '3rdparty:libthrift-0.5.0',
    '3rdparty:mustache-compiler',
    '3rdparty:scopt',
    '3rdparty/jvm/org/codehaus/plexus:plexus-utils',
    'finagle/finagle-core',
    'util/util-codec',
    'util/util-core',
  ],
  sources=rglobs('*.scala'),
  resources=[
    'scrooge/scrooge-generator/src/main/resources:resources'
  ],
)
