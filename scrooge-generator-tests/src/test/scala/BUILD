junit_tests(
  dependencies=[
    '3rdparty/jvm/asm',
    '3rdparty/jvm/cglib',
    '3rdparty/jvm/junit',
    '3rdparty/jvm/org/objenesis',
    '3rdparty/jvm/org/apache/thrift:libthrift-0.5.0',
    '3rdparty/jvm/org/hamcrest:hamcrest-core',
    '3rdparty/jvm/org/jmock',
    '3rdparty/jvm/org/mockito:mockito-all',
    '3rdparty/jvm/org/scala-lang/modules:scala-parser-combinators',
    '3rdparty/jvm/org/scalatest',
    'finagle/finagle-thrift/src/main/java:java',
    'scrooge/scrooge-core',
    'scrooge/scrooge-generator',
    'scrooge/scrooge-generator-tests/src/test/thrift/airport:thrift-android',
    'scrooge/scrooge-generator-tests/src/test/thrift/airport:thrift-namespace-android',
    'scrooge/scrooge-generator-tests/src/test/thrift/airport:thrift-namespace-scala',
    'scrooge/scrooge-generator-tests/src/test/thrift/airport:thrift-scala',
#    'scrooge/scrooge-generator-tests/src/test/thrift/defaults:thrift-android',
    'scrooge/scrooge-generator-tests/src/test/thrift/defaults:thrift-java',
    'scrooge/scrooge-generator-tests/src/test/thrift/defaults:thrift-scala',
    'scrooge/scrooge-generator-tests/src/test/thrift/integration:thrift-android',
    'scrooge/scrooge-generator-tests/src/test/thrift/integration:thrift-java',
    'scrooge/scrooge-generator-tests/src/test/thrift/integration:thrift-scala',
#    'scrooge/scrooge-generator-tests/src/test/thrift/namespace:thrift-android',
#    'scrooge/scrooge-generator-tests/src/test/thrift/namespace:thrift-scala',
    'scrooge/scrooge-generator-tests/src/test/thrift/relative:includes-scala',
#    'scrooge/scrooge-generator-tests/src/test/thrift/relative:thrift-scala',
    'scrooge/scrooge-generator-tests/src/test/thrift/scala:thrift-java',
    'scrooge/scrooge-generator-tests/src/test/thrift/scala:thrift-scala',
#    'scrooge/scrooge-generator-tests/src/test/thrift/standalone:thrift-android',
#    'scrooge/scrooge-generator-tests/src/test/thrift/standalone:thrift-java',
    'scrooge/scrooge-generator-tests/src/test/thrift/standalone:thrift-scala',
#    'scrooge/scrooge-generator-tests/src/test/thrift:constant-sets-thrift-android',
    'scrooge/scrooge-generator-tests/src/test/thrift:constant-sets-thrift-scala',
    'util/util-core/src/main/scala:scala',
  ],
  sources=rglobs(
    '*.scala',
    exclude=[
      # this needs the relative:thrift-scala target
      ['com/twitter/scrooge/android_generator/AndroidGeneratorSpec.scala'],

      # this needs the namespace:thrift-* targets
      ['com/twitter/scrooge/backend/NamespaceSpec.scala'],

      # this needs --default-java-namespace apache_java_thrift for standalone
      ['com/twitter/scrooge/backend/ApacheJavaMetadataSpec.scala'],

      # this needs the relative:thrift-scala target
      ['com/twitter/scrooge/backend/ScalaGeneratorSpec.scala'],

      # this is not respecting the namespace_map in integration:thrift-java
      ['com/twitter/scrooge/integration/ScalaIntegrationSpec.scala'],

      # these tests fail at runtime
      ['com/twitter/scrooge/java_generator/ApacheJavaGeneratorSpec.scala'],
      ['com/twitter/scrooge/cocoa_generator/CocoaGeneratorSpec.scala'],

    ]
  ),
  resources=[
    'scrooge/scrooge-generator-tests/src/test/resources:resources',
    'scrooge/scrooge-generator-tests/src/test/thrift:thrift',
  ],
)
