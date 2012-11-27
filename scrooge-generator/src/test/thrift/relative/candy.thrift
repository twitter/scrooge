/* Specifically written to test various ways of importing the include files
 * Go to the directory of scrooge-generator project and run the following command:
 *    scrooge src/test/thrift/relative/candy.thrift -i src/test/thrift/relative/dir2:src/test/thrift/relative/include3.jar
 */
namespace java thrift.test2


// from relative path specified in file
include "./dir1/../dir1/include1.thrift"

// from relative path "dir2" passed through command line
include "include2.thrift"

// from include3.jar passed through command line
include "include3.thrift"

typedef include1.CandyType CandyType

struct Candy {
  1: i32 sweetness_iso
  2: CandyType candy_type
  3: string headline = include1.HEADLINE
  4: optional string brand = include2.BRAND
  5: i32 count = include3.PIECES
}
