{{#hasConstants}}
// ----- constants

public class Constants {
{{#constants}}
  public static final {{type}} {{name}} = {{value}};
{{/constants}}
}

{{/hasConstants}}
