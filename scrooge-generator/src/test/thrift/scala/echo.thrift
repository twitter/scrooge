#@namespace scala collisions
namespace java java_collisions

/* Method colliding with service name */
service Echo {
  string echo(1: string msg)
}

/* Extending service with a method colliding with service name */
service BetterEcho extends Echo {
  string betterEcho(1: string msg)
}

service Slice {
  i32 action()
}

/* Extending service has a method colliding with the parent service name */
service ExtendedSlice extends Slice {
  i32 slice()
}

/* Extending service name colliding with parent method name */
service Action extends Slice {
  i32 noop()
}
