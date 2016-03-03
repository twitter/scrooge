include "service.thrift"

// Extending a service from a filename that is a keyword is ok.
service MyOtherService extends service.MyService { }
