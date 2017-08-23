java_thrift_library(name='thrift-scala',
  sources=rglobs('*.thrift'),
  compiler='scrooge',
  language='scala',
  rpc_style='finagle',
  strict_deps=True,
)

java_thrift_library(name='thrift-android',
  sources=rglobs('*.thrift'),
  compiler='scrooge',
  language='android',
  rpc_style='finagle',
  dependencies=[
    '3rdparty/jvm/org/apache/thrift:libthrift-0.5.0',
  ],
  strict_deps=True,
)

# namespaced and non-Finagle
java_thrift_library(name='thrift-namespace-scala',
  sources=rglobs('*.thrift'),
  compiler='scrooge',
  language='scala',
  namespace_map={
    'thrift.test': 'vanilla.test',
    'thrift.test1': 'vanilla.test1',
    'thrift.test2': 'vanilla.test2',
  },
  strict_deps=True,
)

java_thrift_library(name='thrift-namespace-android',
  sources=rglobs('*.thrift'),
  compiler='scrooge',
  language='android',
  dependencies=[
    '3rdparty/jvm/org/apache/thrift:libthrift-0.5.0',
  ],
  namespace_map={
    'androidthrift.test': 'vanilla_android.test',
    'androidthrift.test1': 'vanilla_android.test1',
    'androidthrift.test2': 'vanilla_android.test2',
  },
  strict_deps=True,
)
