/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * Contains some contributions under the Thrift Software License.
 * Please see doc/old-thrift-license.txt in the Thrift distribution for
 * details.
 */

/**
 * docs here should be ignored
 */
namespace c_glib TTest
namespace java thrift.test
namespace cpp thrift.test
namespace rb Thrift.Test
namespace perl ThriftTest
namespace csharp Thrift.Test
namespace js ThriftTest
namespace st ThriftTest
namespace py ThriftTest
namespace py.twisted ThriftTest
namespace go ThriftTest
namespace php ThriftTest
namespace * thrift.test

#
# blah
#
/**
 * Docstring!
 */
enum NumberID
{
  One = 1,
  Two,
  Three,
  Five = 5,
  Six,
  Eight = 8
}

struct EnumStruct {
  1: NumberID number
}

union EnumUnion {
  1: NumberID number
  2: string text
}

struct EnumCollections {
  1: map<NumberID, NumberID> aMap
  2: list<NumberID> aList
  3: set<NumberID> aSet
}

enum NamespaceCollisions
{
  List,
  Any,
  AnyRef,
  Object,
  String,
  Byte,
  Short,
  Char,
  Int,
  Long,
  Float,
  Double,
  Option,
  None,
  Some,
  Nil,
  Null,
  Set,
  Map,
  Seq,
  Array,
  Iterable,
  Unit,
  Nothing,
  protected
}

/** doc, ignored */
typedef i64 UserId

struct Ints {
  1: i16 baby
  2: i32 mama
  3: i64 papa
}

/** structdocs */
struct Bytes {
  1: byte x
  /** field docs */
  2: binary y
}

struct Misc {
  1: required bool alive
  2: required double pi
  3: string name
}

struct Compound {
  1: list<i32> intlist
  2: set<i32> intset
  3: map<string, i32> namemap
  4: list<set<i32>> nested
}

struct RequiredString {
  1: required string value
}

struct RequiredStringWithDefault {
  1: required string value = "yo"
}

struct OptionalInt {
  1: string name
  2: optional i32 age
}

struct DefaultValues {
  1: string name = "leela"
}

struct Emperor {
  1: string name
  2: i32 age
}

struct Empire {
  1: string name
  2: list<string> provinces
  5: Emperor emperor
}

struct Biggie {
  1: i32 num1 = 1
  2: i32 num2 = 2
  3: i32 num3 = 3
  4: i32 num4 = 4
  5: i32 num5 = 5
  6: i32 num6 = 6
  7: i32 num7 = 7
  8: i32 num8 = 8
  9: i32 num9 = 9
  10: i32 num10 = 10
  11: i32 num11 = 11
  12: i32 num12 = 12
  13: i32 num13 = 13
  14: i32 num14 = 14
  15: i32 num15 = 15
  16: i32 num16 = 16
  17: i32 num17 = 17
  18: i32 num18 = 18
  19: i32 num19 = 19
  20: i32 num20 = 20
  21: i32 num21 = 21
  22: i32 num22 = 22
  23: i32 num23 = 23
  24: i32 num24 = 24
  25: i32 num25 = 25
}

struct ReallyBig {
   1: Empire empire1
   2: Empire empire2
   3: Empire empire3
   4: Empire empire4
   5: Empire empire5
   6: Empire empire6
   7: Empire empire7
   8: Empire empire8
   9: Empire empire9
  10: Empire empire10
  11: optional Empire empire11
  12: optional Empire empire12
  13: optional Empire empire13
  14: optional Empire empire14
  15: optional Empire empire15
  16: optional Empire empire16
  17: optional Empire empire17
  18: optional Empire empire18
  19: optional Empire empire19
  20: optional Empire empire20
  21: i32 num21 = 21
  22: i32 num22 = 22
  23: i32 num23 = 23
  24: i32 num24 = 24
  25: i32 num25 = 25
  26: i32 num26 = 26
  27: i32 num27 = 27
  28: i32 num28 = 28
  29: i32 num29 = 29
  30: i32 num30 = 30
  31: optional i32 num31
  32: optional i32 num32
  33: optional i32 num33
  34: optional i32 num34
  35: optional i32 num35
  36: optional i32 num36
  37: optional i32 num37
  38: optional i32 num38
  39: optional i32 num49
  40: optional i32 num40
  41: list<Empire> empire_list41
  42: list<Empire> empire_list42
  43: list<Empire> empire_list43
  44: list<Empire> empire_list44
  45: list<Empire> empire_list45
  46: list<Empire> empire_list46
  47: list<Empire> empire_list47
  48: list<Empire> empire_list48
  49: list<Empire> empire_list49
  50: list<Empire> empire_list50
  51: optional list<Empire> empire_list51
  52: optional list<Empire> empire_list52
  53: optional list<Empire> empire_list53
  54: optional list<Empire> empire_list54
  55: optional list<Empire> empire_list55
  56: optional list<Empire> empire_list56
  57: optional list<Empire> empire_list57
  58: optional list<Empire> empire_list58
  59: optional list<Empire> empire_list59
  60: optional list<Empire> empire_list60
}

struct OneOfEach {
  1: bool z
  2: byte b
  3: i16 s
  4: i32 i
  5: i64 j
  6: double d
  7: string str
  8: NumberID e
  9: Misc ref
  10: list<i32> i_list
  11: set<i32> i_set
  12: map<i32, i32> i_map
}

struct OneOfEachWithDefault {
  1: bool z = 1
  2: byte b = 1
  3: i16 s = 1
  4: i32 i = 1
  5: i64 j = 1
  6: double d = 1.0
  7: string str = "yo"
  8: NumberID e = NumberID.One
  10: list<i32> i_list = [1]
  11: set<i32> i_set = [1]
  12: map<i32, i32> i_map = {1: 1}
}

struct OneOfEachOptional {
  1: optional bool z
  2: optional byte b
  3: optional i16 s
  4: optional i32 i
  5: optional i64 j
  6: optional double d
  7: optional string str
  8: optional NumberID e
  9: optional Misc ref
  10: optional list<i32> i_list
  11: optional set<i32> i_set
  12: optional map<i32, i32> i_map
}

struct OneOfEachOptionalWithDefault {
  1: optional bool z = 1
  2: optional byte b = 1
  3: optional i16 s = 1
  4: optional i32 i = 1
  5: optional i64 j = 1
  6: optional double d = 1.0
  7: optional string str = "yo"
  8: optional NumberID e = NumberID.One
  10: optional list<i32> i_list = [1]
  11: optional set<i32> i_set = [1]
  12: optional map<i32, i32> i_map = {1: 1}
}

struct Bonk
{
  1: string message,
  2: i32 type
}

struct Bools {
  1: bool im_true,
  2: bool im_false,
}

struct Xtruct
{
  1:  string string_thing,
  4:  byte   byte_thing,
  9:  i32    i32_thing,
  11: i64    i64_thing
}

struct Xtruct2
{
  1: byte   byte_thing,
  2: Xtruct struct_thing,
  3: i32    i32_thing
}

struct Xtruct3
{
  1:  string string_thing,
  4:  i32    changed,
  9:  i32    i32_thing,
  11: i64    i64_thing
}

struct XtructColl
{
  1: required  map<i32, i64> a_map,
  2: required list<string>  a_list,
  3: required set<byte>     a_set,
  4: required i32           non_col
}

struct NestedXtruct
{
  1: Xtruct x1,
  2: Xtruct2 x2,
  3: Xtruct3 x3
}


struct Insanity
{
  1: map<NumberID, UserId> userMap,
  2: list<Xtruct> xtructs
}

struct CrazyNesting {
  1: string string_field,
  2: optional set<Insanity> set_field,
  3: required list< map<set<i32>,map<i32,set<list<map<Insanity,string>>>>>> list_field,
  4: binary binary_field
}

union MorePerfectUnion {
  1: Bonk bonk
  2: Bools bools
  3: Xtruct xtruct
}

exception EmptyXception { }

exception Xception {
  1: i32 errorCode,
  2: string message
}

/**
 * yep yep, i've documented this
 **/
exception Xception2 {
  1: i32 errorCode,
  2: Xtruct struct_thing
}

// makes sure we generate getMessage on the first string arg,
exception StringMsgException {
  1: i32 something
  2: string use_this
}

// make sure we can handle Throwable's getMessage
exception NonStringMessageException {
  1: i32 message
}

struct EmptyStruct {}

struct OneField {
  1: EmptyStruct field
}

/**
 * smpfy
 */
service SimpleService {
  i32 deliver(1: string where)
}

service ExceptionalService {
  i32 deliver(1: string where) throws (
    1: Xception ex
    2: Xception2 ex2
    3: EmptyXception ex3
  )

  void remove(1: i32 id) throws (
    1: Xception ex
    2: Xception2 ex2
    3: EmptyXception ex3
  )
}

service ThriftTest
{
  void         testVoid(),
  string       testString(1: string thing),
  byte         testByte(1: byte thing),
  i32          testI32(1: i32 thing),
  i64          testI64(1: i64 thing),
  double       testDouble(1: double thing),
  Xtruct       testStruct(1: Xtruct thing),
  Xtruct2      testNest(1: Xtruct2 thing),
  map<i32,i32> testMap(1: map<i32,i32> thing),
  set<i32>     testSet(1: set<i32> thing),
  list<i32>    testList(1: list<i32> thing),
  NumberID      testEnum(1: NumberID thing),
  UserId       testTypedef(1: UserId thing),

  /** double doc comments don't cause problems, right? */
  /** now i've fulfilled my documentation requirements
   */
  map<i32,map<i32,i32>> testMapMap(1: i32 hello),

  /* So you think you've got this all worked, out eh? */
  map<UserId, map<NumberID,Insanity>> testInsanity(1: Insanity argument),

  /* Multiple parameters */
  Xtruct testMulti(
    1: byte arg0,
    /** function parameter doc */
    2: i32 arg1,
    3: i64 arg2,
    4: map<i16, string> arg3,
    5: NumberID arg4,
    6: UserId arg5
  ),

  /* Exception specifier */

  void testException(1: string arg) throws(1: Xception err1),

  /* Multiple exceptions specifier */

  Xtruct testMultiException(1: string arg0, 2: string arg1) throws(1: Xception err1, 2: Xception2 err2),

  /* Unions */

  MorePerfectUnion testUnions(1: MorePerfectUnion arg0)
}

struct VersioningTestV1 {
       1: i32 begin_in_both,
       3: string old_string,
       12: i32 end_in_both
}

struct VersioningTestV2 {
       1: i32 begin_in_both,

       2: i32 newint,
       3: byte newbyte,
       4: i16 newshort,
       5: i64 newlong,
       6: double newdouble
       7: Bonk newstruct,
       8: list<i32> newlist,
       9: set<i32> newset,
       10: map<i32, i32> newmap,
       11: string newstring,
       12: i32 end_in_both
}

struct ListTypeVersioningV1 {
       1: list<i32> myints;
       2: string hello;
}

struct ListTypeVersioningV2 {
       1: list<string> strings;
       2: string hello;
}

struct GuessProtocolStruct {
  7: map<string,string> map_field,
}

struct LargeDeltas {
  1: Bools b1,
  10: Bools b10,
  100: Bools b100,
  500: bool check_true,
  1000: Bools b1000,
  1500: bool check_false,
  2000: VersioningTestV2 vertwo2000,
  2500: set<string> a_set2500,
  3000: VersioningTestV2 vertwo3000,
  4000: list<i32> big_numbers
}

service ReadOnlyService {
  string getName()
}

service ReadWriteService extends ReadOnlyService {
  void setName(1: string name)
}

service Capsly {
  string Bad_Name()
}
