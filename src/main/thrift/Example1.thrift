namespace java com.twitter.scrooge.generated

exception Example1Exception {
  1: string description
}

service Example1 {
  i32 get_id(1: string name) throws(Example1Exception ex)
}
