java_thrift_library(name='thrift-scala',
  sources=rglobs('*.thrift'),
  compiler='scrooge',
  language='scala',
)

java_thrift_library(name='thrift-java',
  sources=rglobs('*.thrift'),
  compiler='scrooge',
  language='java',
)


java_thrift_library(name='thrift-android',
  sources=rglobs('*.thrift'),
  compiler='scrooge',
  language='android',
  dependencies=[
    '3rdparty/jvm/org/apache/thrift:libthrift-0.5.0',
  ],
  default_java_namespace='apache_java_thrift',
)
