namespace java mythrift.bird

enum Location {
  Africa,
  America,
  Antarctica,
  Asia,
  Australia,
  Europe
}

struct Bird {
  1: bool canFly
  2: string habitat
}

service BirdService {
  list<Bird> getNonFlyingBirds(1: Location habitat)
}
