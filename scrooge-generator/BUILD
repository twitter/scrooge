maven_layout()

jar_library(name='scrooge-generator',
  dependencies=[
    'scrooge/scrooge-generator/src/main/scala'
  ]
)

jar_library(name='tests',
  dependencies=[
    'scrooge/scrooge-generator/src/test/scala'
  ]
)

jvm_binary(
  name='bin',
  main='com.twitter.scrooge.Main',
  dependencies=[
    'scrooge/scrooge-generator/src/main/scala'
  ]
)
