scala_library(name='scala',
  provides = artifact(
    org = 'com.twitter',
    name = 'scrooge-core',
    repo = pants('pants-support/ivy:internal'),
  ),
  dependencies=[
    pants('3rdparty:libthrift-0.5.0')
  ],
  sources=rglobs('*.scala'),
)
