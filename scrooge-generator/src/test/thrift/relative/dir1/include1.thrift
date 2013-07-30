// include1.thrift
// to be imported to candy.thrift by specifying relative path in the file, i.e.:
//     include "dir1/../dir1/include1.thrift
namespace java thrift.test1

// When compiling include1.thrift, the updated importer should contain
// the "dir1" directory in its import paths, which is used to locate
// "dir4/include4.thrift".
include "dir4/include4.thrift"

enum CandyType {
  OLD = 0,
  deliCIous = 1,
  WEIRD = 2
}

const string HEADLINE = include4.line2