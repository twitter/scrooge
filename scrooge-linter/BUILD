maven_layout()

jar_library(
  name='scrooge-linter',
  dependencies=[
    pants('scrooge/scrooge-linter/src/main/scala/com/twitter/scrooge/linter')
  ],
)

jar_library(
  name='tests',
  dependencies=[
    pants('scrooge/scrooge-linter/src/main/scala/com/twitter/scrooge/linter')
  ]
)

jvm_binary(
  name='linter-bin',
  main='com.twitter.scrooge.Main',
  dependencies=[
    pants('scrooge/scrooge-generator/src/main/scala/BUILD:scala'),
    pants(':scrooge-linter'),
  ]
)
