include "c1.thrift"
include "c2.thrift"
include "c3.thrift"

service TopService { i32 getNumber(1: string someInput) }
