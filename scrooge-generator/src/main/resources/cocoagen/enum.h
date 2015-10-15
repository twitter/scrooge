#import <Foundation/Foundation.h>

typedef NS_ENUM(NSInteger, {{EnumName}}) {
{{#values}}
  {{EnumName}}_{{originalName}} = {{value}}{{/values|,
}}

};

