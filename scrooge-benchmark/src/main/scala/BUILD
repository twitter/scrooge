scala_library(
  dependencies=[
    '3rdparty/jvm/org/apache/thrift:libthrift-0.5.0',
    '3rdparty/jvm/org/openjdk/jmh:jmh-core',
    'scrooge/scrooge-adaptive',
    'scrooge/scrooge-adaptive/src/test/scala/com/twitter/scrooge/adapt/testutil',
    'scrooge/scrooge-benchmark/src/main/thrift:thrift-scala',
    'scrooge/scrooge-core/src/main/scala:scala',
    'scrooge/scrooge-serializer',
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala'),
  exports=[
    '3rdparty/jvm/org/openjdk/jmh:jmh-core',
  ]

)

jvm_binary(name='jmh',
  main='org.openjdk.jmh.Main',
  dependencies=[
    ':scala'
  ]
)
