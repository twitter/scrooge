namespace java com.twitter.test
#@namespace scala com.twitter.test

include "included.thrift"

# Missing includes are ok.
include "nonexistent.thrift"
