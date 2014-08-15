maven_layout()

scala_library(
  name='scala',
  dependencies=[
    pants('3rdparty:libthrift-0.5.0'),
    pants('3rdparty/jvm/com/twitter/servo:thrift-scala'),
    pants('scrooge/scrooge-generator'),
    pants('util/util-logging'),
  ],
  sources=globs('*.scala'),
  resources=[
    pants('scrooge/scrooge-linter/src/main/resources')
  ],
)

jvm_binary(
  name='bin',
  basename='scrooge-linter',
  main='com.twitter.scrooge.linter.Main',
  dependencies=[pants(':scala')]
)
