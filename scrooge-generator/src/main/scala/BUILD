scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'scrooge-generator',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/github/scopt',
    '3rdparty/jvm/com/github/spullara/mustache/java:mustache-compiler',
    '3rdparty/jvm/org/codehaus/plexus:plexus-utils',
    '3rdparty:commons-cli',
    '3rdparty:guava',
    '3rdparty:libthrift-0.5.0',
    '3rdparty:scala-library',
    'finagle/finagle-core',
    'util/util-codec',
    'util/util-core',
  ],
  sources=rglobs('*.scala'),
  resources=[
    'scrooge/scrooge-generator/src/main/resources:resources'
  ],
)
