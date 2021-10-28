package com.twitter.scrooge.validation;

import com.twitter.scrooge.ThriftValidator;

public class JCustomValidator {

  public ThriftValidator getThriftValidator() {
    java.util.Map<String, ThriftConstraintValidator<?, ?>> customConstraints =
        new java.util.HashMap<>();
    customConstraints.put("validation.testStartWithA", new JStartWithAConstraintValidator());
    customConstraints.put("validation.testUserId", new JTestUserIdConstraintValidator());
    customConstraints.put("validation.testScreenName", new JScreenNameConstraintValidator());
    return ThriftValidator.newBuilder().withConstraints(customConstraints).build();
  }

  static class JStartWithAConstraintValidator implements ThriftConstraintValidator<String, String> {
    public Class<String> annotationClazz() {
      return String.class;
    }

    public String violationMessage(String obj, String annotation) {
      return "must start with a";
    }

    public boolean isValid(String obj, String annotation) {
      return obj.startsWith("a");
    }
  }

  static class JTestUserIdConstraintValidator implements ThriftConstraintValidator<Long, String> {

    public Class<String> annotationClazz() {
      return String.class;
    }

    public String violationMessage(Long obj, String annotation) {
      return "invalid user id";
    }

    public boolean isValid(Long obj, String annotation) {
      return obj > 11111 && obj < 22222;
    }
  }

  static class JScreenNameConstraintValidator implements ThriftConstraintValidator<String, String> {
    public Class<String> annotationClazz() {
      return String.class;
    }

    public String violationMessage(String obj, String annotation) {
      return "invalid user screen name";
    }

    public boolean isValid(String obj, String annotation) {
      return obj.startsWith("@");
    }
  }
}
