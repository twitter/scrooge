namespace java thrift.test

// this service will blow up during compile unless "binary" types work.
service BinaryService {
  binary fetchBlob(1: i64 id)
}
