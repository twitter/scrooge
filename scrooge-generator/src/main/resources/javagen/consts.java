package {{package}};

import com.twitter.scrooge.util.Utilities;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Constants {
{{#constants}}
  {{docstring}}
  public static final {{fieldType}} {{name}} = {{value}};
{{/constants}}
}