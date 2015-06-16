maven_layout()

target(name='scrooge-generator',
  dependencies=[
    'scrooge/scrooge-generator/src/main/scala'
  ]
)

target(name='tests',
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
