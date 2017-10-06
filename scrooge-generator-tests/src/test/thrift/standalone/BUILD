java_thrift_library(name='thrift-scala',
  sources=rglobs('*.thrift'),
  compiler='scrooge',
  language='scala',
)

java_thrift_library(name='thrift-android',
  sources=['enumSet.thrift', 'exception_fields.thrift'],
  compiler='scrooge',
  language='android',
  dependencies=[
    '3rdparty/jvm/org/apache/thrift:libthrift-0.5.0',
  ],
  default_java_namespace='android_thrift_default_namespace',
)

java_thrift_library(name='thrift-java',
  sources=['enumSet.thrift', 'exception_fields.thrift'],
  compiler='scrooge',
  language='java',
  namespace_map={
   'thrift.test': 'thrift.apache_java_test',
  },
  default_java_namespace='apache_java_thrift',
)
