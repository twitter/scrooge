scala_library(name='scala',
  dependencies=[
    '3rdparty/jvm/org/apache/thrift:libthrift-0.5.0',
    '3rdparty/jvm/org/openjdk/jmh:jmh-core',
    'scrooge/scrooge-benchmark/src/main/thrift:thrift-scala',
    'scrooge/scrooge-serializer',
  ],
  sources=rglobs('*.scala')
)

jvm_binary(name='jmh',
  main='org.openjdk.jmh.Main',
  dependencies=[
    ':scala'
  ]
)
