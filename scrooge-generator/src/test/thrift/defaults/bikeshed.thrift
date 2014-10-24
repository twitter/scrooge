namespace java thrift.defaults

include "colors.thrift"

struct Bikeshed {
  colors.Color primaryColor = colors.Color.Red
  colors.Color otherColor = colors.Color.WHITE
}
