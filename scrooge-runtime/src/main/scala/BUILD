scala_library(name='scala',
  provides = artifact(
    org = 'com.twitter',
    name = 'scrooge-runtime',
    repo = pants('pants-support/ivy:internal'),
  ),
  dependencies=[
    pants('finagle/finagle-core'),
    pants('finagle/finagle-thrift'),
    pants('util/util-core'),
    pants('scrooge/scrooge-core')
  ],
  sources=rglobs('*.scala'),
)
