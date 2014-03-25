namespace java thrift.constants

const set<string> VALID_IDENTIFIERS = ["devel",
                                       "dev-prod",
                                       "Dev_prod-",
                                       "deV.prod",
                                       ".devprod.."]

const set<string> INVALID_IDENTIFIERS = ["dev/prod",
                                         "dev prod",
                                         "/dev/prod",
                                         "///",
                                         "new\nline",
                                         "    hello world."]

const string STRINGWITHSPECIALCHARS = "^[\\w\\-\\.]+$"

const string STRINGWITHSPECIALCHARS2 = "\\\n\r\t"

const string STRINGWITHSPECIALCHARS3 = "ãáíô"
