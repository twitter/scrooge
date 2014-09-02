scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'scrooge-ostrich',
    repo = pants('pants-support/ivy:internal'),
  ),
  dependencies=[
    pants('3rdparty:libthrift-0.5.0'),
    pants('finagle/finagle-core'),
    pants('finagle/finagle-ostrich4'),
    pants('finagle/finagle-thrift'),
    pants('finagle/finagle-thriftmux'),
    pants('util/util-app'),
    pants('util/util-core')
  ],
  sources=rglobs('*.scala'),
)
