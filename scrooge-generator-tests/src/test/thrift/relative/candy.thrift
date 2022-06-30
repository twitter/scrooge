/* Specifically written to test various ways of importing the include files
 * Go to the directory of scrooge-generator project and run the following command:
 *    scrooge src/test/thrift/relative/candy.thrift -i src/test/thrift/relative/dir2:src/test/thrift/relative/dir3
 */
#@namespace android thrift.android.test2
namespace java thrift.test2


// from relative path specified in file
include "./dir1/../dir1/include1.thrift"

// from relative path "dir2" passed through command line
include "./dir2/include2.thrift"

// from relative path "dir3" passed through command line
include "./dir3/include3.thrift"

typedef include1.CandyType CandyType

struct Candy {
  1: i32 sweetness_iso
  2: CandyType candy_type
  3: string headline = include1.HEADLINE
  4: optional string brand = include2.BRAND
  5: i32 count = include3.PIECES
  6: CandyType default_candy_type = CandyType.OLD
}
