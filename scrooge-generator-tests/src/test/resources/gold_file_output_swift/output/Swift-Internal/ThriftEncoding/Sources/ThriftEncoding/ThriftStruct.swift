//
// Autogenerated by Scrooge
//
// DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
//
import Foundation
import TwitterApacheThrift
struct ThriftStruct: Hashable {
    var int16Value: Int16?
    enum CodingKeys: Int, CodingKey {
        case int16Value = 1
    }
    init(int16Value: Int16? = nil) {
        self.int16Value = int16Value
    }
}
extension ThriftStruct: ThriftCodable {}