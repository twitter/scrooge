maven_layout()

jar_library(name='scrooge-generator',
  dependencies=[
    pants('scrooge/scrooge-generator/src/main/scala')
  ]
)

jvm_binary(
  name='bin',
  main='com.twitter.scrooge.Main',
  dependencies=[
    pants('scrooge/scrooge-generator/src/main/scala')
  ]
)
