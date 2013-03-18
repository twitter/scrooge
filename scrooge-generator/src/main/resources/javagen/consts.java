package {{package}};

import com.twitter.scrooge.Utilities;
import java.util.List;
import java.util.Map;
import java.util.Set;

@javax.annotation.Generated(value = "com.twitter.scrooge.Compiler", date = "{{date}}")
public final class Constants {
{{#constants}}
  {{docstring}}
  public static final {{fieldType}} {{name}} = {{value}};
{{/constants}}
}