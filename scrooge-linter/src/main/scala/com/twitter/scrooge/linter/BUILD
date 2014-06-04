maven_layout()

scala_library(
  name='linter',
  dependencies=[
    pants('3rdparty:libthrift-0.5.0'),
    pants('3rdparty/jvm/com/twitter/servo:thrift-scala'),
    pants('scrooge/scrooge-generator'),
  ],
  sources=globs('*.scala'),
  resources=[
    pants('scrooge/scrooge-linter/src/main/resources')
  ],
)
