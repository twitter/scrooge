namespace java thrift.collision

/* Naming a struct Product(N) will collide with scala.Product(N) in a few places.
 * The victims below will not build properly if uses of scala.Product are not fully qualified. */

struct Product { }

//will not build if collision with Product exists
struct VictimOfScalaDotProductNameCollision { }


struct Product1 { }

//will not build if collision with Product1 exists
struct VictimOfScalaDotProduct1NameCollision {
  1: i64 giveVictimArity1
}

// Defined to test productElement for fields named "n"
struct ProductElementStruct {
  1: string a
  2: string n
}
