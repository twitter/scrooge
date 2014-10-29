scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'scrooge-ostrich',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty:libthrift-0.5.0',
    'finagle/finagle-core',
    'finagle/finagle-ostrich4',
    'finagle/finagle-thrift',
    'finagle/finagle-thriftmux',
    'util/util-app',
    'util/util-core'
  ],
  sources=rglobs('*.scala'),
)
