java_thrift_library(name='includes-scala',
  sources=[
    'dir1/dir4/include4.thrift',
    'dir1/include1.thrift',
    'dir2/include2.thrift',
    'dir3/include3.thrift',
  ],
  compiler='scrooge',
  language='scala',
  rpc_style='finagle',
)

# todo: add android
# todo: this isn't working
## https://github.com/pantsbuild/pants/blob/5dd58b845977bc6bce9b13b6e57006811a2443a7/contrib/scrooge/src/python/pants/contrib/scrooge/tasks/thrift_linter.py#L84
#java_thrift_library(name='thrift-scala',
#  # ok this goes to the --include-path for the linter but we need it to be directories
#  dependencies=[
#    'dir2', 'dir3',
#  ],
#  sources=['candy.thrift'],
#  compiler='scrooge',
#  language='scala',
#  rpc_style='finagle',
#)
