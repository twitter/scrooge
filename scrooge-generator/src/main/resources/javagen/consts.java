package {{package}};

import com.twitter.scrooge.Utilities;
import java.util.List;
import java.util.Map;

public final class Constants {
{{#constants}}
  {{docstring}}
  public static final {{type}} {{name}} = {{value}};
{{/constants}}
}