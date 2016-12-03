resources(
  sources=rglobs('*', exclude=[rglobs('BUILD*')])
)

java_thrift_library(name='constant-sets-thrift-scala',
  sources=['constant_sets.thrift'],
  compiler='scrooge',
  language='scala',
  rpc_style='finagle',
)

# todo: this needs a --default-java-namespace
#java_thrift_library(name='constant-sets-thrift-android',
#  sources=['constant_sets.thrift'],
#  compiler='scrooge',
#  language='android',
#  rpc_style='finagle',
#  dependencies=[
#    '3rdparty/jvm/org/apache/thrift:libthrift-0.5.0',
#  ],
#)
