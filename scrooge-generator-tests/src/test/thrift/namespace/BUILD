java_thrift_library(name='thrift-scala',
  sources=rglobs('*.thrift'),
  compiler='scrooge',
  language='scala',
  default_java_namespace='bar',
  strict_deps=True,
)

java_thrift_library(name='thrift-android',
  sources=rglobs('*.thrift'),
  compiler='scrooge',
  language='android',
  dependencies=[
    '3rdparty/jvm/org/apache/thrift:libthrift-0.5.0',
  ],
  namespace_map={
    'foo': 'android_foo',
    'bar': 'android_bar',
    'com.fake': 'com.android_fake',
  },
  default_java_namespace='android_bar',
  strict_deps=True,
)
