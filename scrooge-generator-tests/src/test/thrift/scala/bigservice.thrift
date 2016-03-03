#@namespace scala collisions.big.thriftscala
namespace java collisions.big.thriftjava


# Scala 2.10 can't handle case classes with 22+ arguments.
service BigService {
  i32 getNumber0();
  i32 getNumber1();
  i32 getNumber2();
  i32 getNumber3();
  i32 getNumber4();
  i32 getNumber5();
  i32 getNumber6();
  i32 getNumber7();
  i32 getNumber8();
  i32 getNumber9();
  i32 getNumber10();
  i32 getNumber11();
  i32 getNumber12();
  i32 getNumber13();
  i32 getNumber14();
  i32 getNumber15();
  i32 getNumber16();
  i32 getNumber17();
  i32 getNumber18();
  i32 getNumber19();
  i32 getNumber20();
  i32 getNumber21();
  i32 getNumber22();
  i32 getNumber23();
  i32 getNumber24();
  i32 getNumber25();
  i32 getNumber26();
  i32 getNumber27();
  i32 getNumber28();
  i32 getNumber29();
  i32 getNumber30();
}

service BiggerService extends BigService {
  i32 oneUp();
}
