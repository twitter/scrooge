namespace java com.twitter.demo

struct User {
  1: i64 id
  2: string name
}

service UserService {
  User createUser(1: string name)
}

