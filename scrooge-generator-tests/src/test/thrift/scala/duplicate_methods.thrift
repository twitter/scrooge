#@namespace scala collisions.dupes.thriftscala
namespace java collisions.dupes.thriftjava
#@namespace typescript collisions.dups.thriftts


service Aaa {
  i32 duplicated();
}

service Bbb extends Aaa {
  i32 duplicated(1: i32 input);
}

service Ccc extends Bbb {
  i32 duplicated(1: string input);
}
