java_thrift_library(name='thrift-scala',
  sources=rglobs('*.thrift'),
  compiler='scrooge',
  language='scala',
  rpc_style='finagle',
)

java_thrift_library(name='thrift-java',
  sources=rglobs('*.thrift'),
  compiler='scrooge',
  language='java',
  rpc_style='finagle',
)


java_thrift_library(name='thrift-android',
  sources=rglobs('*.thrift'),
  compiler='scrooge',
  language='android',
  rpc_style='finagle',
  dependencies=[
    '3rdparty/jvm/org/apache/thrift:libthrift-0.5.0',
  ],
  default_java_namespace='apache_java_thrift',
)
