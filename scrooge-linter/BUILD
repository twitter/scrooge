maven_layout()

jar_library(name='bin',
  dependencies=[pants('scrooge/scrooge-linter/src/main/scala/com/twitter/scrooge/linter:bin')],
)

jvm_app(
  name='app',
  basename='scrooge-linter-package',
  binary=pants(':bin'),
  bundles=[],
)
