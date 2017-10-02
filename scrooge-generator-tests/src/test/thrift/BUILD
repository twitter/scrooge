resources(
  sources=rglobs('*', exclude=[rglobs('BUILD*')])
)

java_thrift_library(name='constant-sets-thrift-scala',
  sources=['constant_sets.thrift'],
  compiler='scrooge',
  language='scala',
  strict_deps=True,
)

java_thrift_library(name='constant-sets-thrift-android',
  sources=['constant_sets.thrift'],
  compiler='scrooge',
  language='android',
  dependencies=[
    '3rdparty/jvm/org/apache/thrift:libthrift-0.5.0',
  ],
  default_java_namespace='android_thrift_default_namespace',
  strict_deps=True,
)
