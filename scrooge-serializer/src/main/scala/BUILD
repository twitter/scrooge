scala_library(name='scala',
  provides = artifact(
    org = 'com.twitter',
    name = 'scrooge-serializer',
    repo = pants('pants-support/ivy:internal'),
  ),
  dependencies=[
    pants('3rdparty:libthrift-0.5.0'),
    pants('util/util-codec'),
    pants('util/util-core'),
    pants('scrooge/scrooge-core')
  ],
  sources=rglobs('*.scala'),
)
