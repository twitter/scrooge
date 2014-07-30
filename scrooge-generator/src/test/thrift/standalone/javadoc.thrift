namespace java thrift.test

/**
 * Javadoc before a service ends up as a doc in generated Scala code.
 */
service TestService {
  list<i32> call(1: string name)
}

/**
 * Trailing javadoc is ok (it's ignored).
 */

/**
 * Repeated trailing javadoc is fine too.
 */
