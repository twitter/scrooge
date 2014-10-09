scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'scrooge-runtime',
    repo = artifactory,
  ),
  dependencies=[
    'finagle/finagle-core',
    'finagle/finagle-thrift',
    'util/util-core',
    'scrooge/scrooge-core'
  ],
  sources=rglobs('*.scala'),
)
