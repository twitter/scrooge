maven_layout()

scala_library(name='scala',
  provides = artifact(
    org = 'com.twitter',
    name = 'scrooge-generator',
    repo = pants('pants-support/ivy:internal'),
  ),
  dependencies=[
    pants('3rdparty:scala-library'),
    pants('3rdparty:commons-cli'),
    pants('3rdparty:guava'),
    pants('3rdparty:libthrift-0.5.0'),
    pants('3rdparty:mustache-compiler'),
    pants('3rdparty:scopt'),
    pants('3rdparty/jvm/org/codehaus/plexus:plexus-utils'),
    pants('finagle/finagle-core'),
    pants('util/util-codec'),
    pants('util/util-core'),
  ],
  sources=rglobs('*.scala'),
  resources=[
    pants('scrooge/scrooge-generator/src/main/resources:resources')
  ],
)
