public enum {{enum_name}} {
{{#values}}
  {{name}}({{value}}){{/values|,
}};

  private final int value;

  private {{enum_name}}(int value) {
    this.value = value;
  }

  /**
   * Get the integer value of this enum value, as defined in the Thrift IDL.
   */
  public int getValue() {
    return value;
  }

  /**
   * Find a the enum type by its integer value, as defined in the Thrift IDL.
   * @return null if the value is not found.
   */
  public static {{enum_name}} findByValue(int value) {
    switch(value) {
{{#values}}
      case {{value}}: return {{name}};
{{/values}}
      default: return null;
    }
  }
}