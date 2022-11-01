Thrift Validation
=================

Data validation is a common task for many services, and there is a core
set of validations that every service wants to do. It can be time consuming
and error prone to define and perform validation in each service.

Thrift Validation is a library and Scrooge compiler integration that validates
Thrift requests for Scrooge generated Finagle services. It provides a core set
of validation constraints by default. It also allows users to define custom
validation constraints to suit different business needs. When Thrift
Validation is enabled, data validation for a Thrift request will be performed
before the request is processed by the service. Utilizing Thrift Validation
library could reduce the amount of duplicated validation definitions, and
identify or stop invalid requests before they even reach services.

Enable Thrift Validation
------------------------

To enable Thrift Validation, please annotate any fields from a `struct`,
`union`, or `exception` in a Thrift IDL. For example:

.. code:: thrift

    struct ValidationStruct {
      1: string stringField (validation.length.min = "6", validation.email = "")
      2: i32 intField (validation.positiveOrZero = "")
      3: i64 longField (validation.max = "100")
      4: i16 shortField (validation.negative = "")
      5: i8 byteField (validation.positive = "")
      6: map<string, string> mapField (validation.size.max = "1")
      7: bool boolField (validation.assertTrue = "")
      8: required string requiredField
      9: optional string optionalField
    }

    struct NestedValidationStruct {
      1: string stringField (validation.email = "")
      2: ValidationStruct nestedStructField
      3: list<ValidationStruct> nestedStructSet (validation.size.max = "1")
    }

    union ValidationUnion {
      1: i32 unionIntField (validation.positiveOrZero = "")
      2: string unionStringField (validation.notEmpty = "")
    }

    exception ValidationException {
      1: string excField (validation.notEmpty = "")
    }

The annotations should be in the format of `validation.<suffix> = <criteria>`,
where `criteria` can be empty for annotations that don’t require a value
(e.g., `validation.notEmpty`, `validation.positive`, etc.). Annotations can be
applied to any base types or containers on a `struct`, `union`, or `exception`
in an IDL. When annotations are applied on container types, the data
validation will be performed against the container value, instead of the
internal values inside the container. Nested validations are supported.

.. note::

    You can apply multiple validation annotations on the same field (see
    annotations for `stringField` in `ValidationStruct` from the above example).
    When doing so, the Thrift Validation library will run all applied annotations
    and return all violations to you regardless of the order of the annotations.

Default validation constraints
------------------------------

Thrift Validation library provides the following built-in validation
constraints.

.. list-table::
   :class: .table .table-striped .table-hover
   :header-rows: 1
   :widths: 5, 2, 5, 1, 5

   * - **Annotation Key**
     - **Field Data Types**
     - **Validation Function**
     - **Annotation Criteria Data Type**
     - **Example Usage**

   * - validation.assertFalse
     - boolean
     - Checks that the annotated element is false.
     - N/A
     - validation.assertFalse = “”

   * - validation.assertTrue
     - boolean
     - Checks that the annotated element is true.
     - N/A
     - validation.assertTrue = “”

   * - validation.countryCode
     - string
     - Checks if the annotated element is a valid `country code <https://www.iso.org/iso-3166-country-codes.html>`_.
     - N/A
     - validation.countryCode = “”

   * - validation.EAN
     - string
     - Checks that the annotated character sequence is a valid `EAN barcode <https://en.wikipedia.org/wiki/International_Article_Number>`_.
     - N/A
     - validation.EAN = “”

   * - validation.email
     - string
     - Checks whether the annotated element is a well formed email address.
     - N/A
     - validation.email = “”

   * - validation.ISBN
     - string
     - Checks that the annotated character sequence is a valid `ISBN <https://en.wikipedia.org/wiki/ISBN>`_.
     - N/A
     - validation.ISBN = “”

   * - validation.length.max
     - string
     - Checks whether the annotated character sequence has length less than or equal to the specified maximum.
     - integer
     - Validation.length.max = “100”

   * - validation.length.min
     - string
     - Checks whether the annotated character sequence has length greater than or equal to the specified minimum.
     - integer
     - validation.length.min = “1”

   * - validation.max
     - i8, i16, i32, i64, double
     - Checks whether the annotated element is less than or equal to the specified maximum.
     - long
     - validation.max = “100”

   * - validation.min
     - i8, i16, i32, i64, double
     - Checks whether the annotated element is greater than or equal to the specified minimum.
     - long
     - validation.min = “1”

   * - validation.negative
     - i8, i16, i32, i64, double
     - Checks if the annotated element is strictly negative. Zero values are considered invalid.
     - N/A
     - validation.negative = “”

   * - validation.negativeOrZero
     - i8, i16, i32, i64, double
     - Checks whether the annotated element is negative or zero.
     - N/A
     - validation.negativeOrZero = “”

   * - validation.notEmpty
     - list, set, map, string
     - Checks whether the annotated element is empty.
     - N/A
     - validation.notEmpty = “”

   * - validation.positive
     - i8, i16, i32, i64, double
     - Checks whether the annotated element is strictly positive. Zero values are considered invalid.
     - N/A
     - validation.positive = “”

   * - validation.positiveOrZero
     - i8, i16, i32, i64, double
     - Checks whether the annotated element is positive or zero.
     - N/A
     - validation.positiveOrZero = “”

   * - validation.size.max
     - list, set, map
     - Checks if the annotated element’s size is less than or equal to the specified maximum.
     - integer
     - validation.size.max = “100”

   * - validation.size.min
     - list, set, map
     - Checks if the annotated element’s size is greater than or equal to the specified minimum.
     - integer
     - validation.size.min = “1”

   * - validation.UUID
     - string
     - Checks whether the annotated element is a universally unique identifier as in java.util.UUID.
     - N/A
     - validation.UUID = “”

.. note::

    When the criteria is not applicable to the annotation, any specified
    criteria value will be ignored. We recommend using an empty string as the
    criteria for such annotations to avoid confusion.

    If any built-in validation constraints are applied to a field with an
    unsupported data type, or the annotation criteria is specified with
    unsupported data types, an error will be returned during code generation.

Define custom validation constraints
------------------------------------

If none of the built-in constraints suffice, you can define custom constraints
to implement any specific validation requirements by following the below steps:

Define an annotation
~~~~~~~~~~~~~~~~~~~~

The new annotation, same as built-in annotations, will be used to annotate any
field from a `struct`, `union`, or `exception` in a Thrift IDL. We recommend
following the  `validation.<suffix> = <criteria>` format to prefix the
annotation key with `validation.` for consistency.

For example, if you want to validate if a string field starts with letter `A`,
you can define an annotation `validation.startWithA = “”`, and apply the
annotation in the IDL:

.. code:: thrift

    struct CustomValidationStruct {
      1: string email (validation.startWithA = "")
    }

Implement a `ThriftConstraintValidator`
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Having defined an annotation, we now need to create an implementation by
extending `ThriftConstraintValidator <https://github.com/twitter/scrooge/blob/b9bc55099d0764bf6061b91c86bc006c33510b1d/scrooge-thrift-validation/src/main/scala/com/twitter/scrooge/thrift_validation/ThriftConstraintValidator.scala>`_:

In Scala:

.. code:: scala

    import com.twitter.scrooge.thrift_validation.ThriftConstraintValidator

    object StartWithAConstraintValidator extends ThriftConstraintValidator[String, String] {

      /** Annotation value is not required for this constraint validator. */
      override def annotationClass: Class[String] = classOf[String]

      override def violationMessage(
        obj: String,
        annotation: String
      ): String = "must start with a"

      /** Return true as long as the given `obj` starts with "a". */
      override def isValid(
        obj: String,
        annotation: String
      ): Boolean = obj.startsWith("a")

      override def fieldClass: Class[String] = classOf[String]
    }

In Java:

.. code:: java

    import com.twitter.scrooge.thrift_validation.ThriftConstraintValidator;

    static class StartWithAConstraintValidator implements ThriftConstraintValidator<String, String> {
      public Class<String> annotationClass() {
        return String.class;
      }

      @Override
      public Class<String> fieldClass() {
        return String.class;
      }

      public String violationMessage(String obj, String annotation) {
        return "must start with a";
      }

      public boolean isValid(String obj, String annotation) {
        return obj.startsWith("a");
      }
    }

.. note::

    The return value of method `violationMessage` will be used for auditing,
    please make sure to not include any PII data in the returned value.

Implement `ThriftValidator`
~~~~~~~~~~~~~~~~~~~~~~~~~~~

The next step is to map the new validation annotation to its implementation
by extending a `ThriftValidator <https://github.com/twitter/scrooge/blob/b9bc55099d0764bf6061b91c86bc006c33510b1d/scrooge-thrift-validation/src/main/scala/com/twitter/scrooge/thrift_validation/ThriftValidator.scala>`_:

In Scala:

.. code:: scala

    package com.twitter.scrooge_internal.thrift_validation.example

    import com.twitter.scrooge.thrift_validation.ThriftValidator

    class CustomValidatorExample extends ThriftValidator {
      override def customAnnotations: Map[String, ThriftConstraintValidator[_, _]] =
        Map(
          "validation.startWithA" -> StartWithAConstraintValidator
        )
    }

In Java:

.. code:: java

    package com.twitter.scrooge_internal.thrift_validation.example;

    import com.twitter.scrooge.thrift_validation.ThriftValidator;

    public class CustomJavaValidatorExample extends ThriftValidator {

      @Override
      public Map<String, ThriftConstraintValidator<?, ?>> customAnnotations() {
        java.util.Map<String, ThriftConstraintValidator<?, ?>> customConstraints =
            new java.util.HashMap<>();
        customConstraints.put("validation.startWithA", new JStartWithAConstraintValidator());
            return toScalaMap(customConstraints);
      }

Provide `ThriftValidator` class name
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The last step is to signal the Thrift Validation library the fully qualified
class name (FQCN) of the new custom `ThriftValidator <https://github.com/twitter/scrooge/blob/b9bc55099d0764bf6061b91c86bc006c33510b1d/scrooge-thrift-validation/src/main/scala/com/twitter/scrooge/thrift_validation/ThriftValidator.scala>`_
in the same IDL where the custom annotation is applied. Please provide the FQCN
after `#@validator` annotation in the beginning of the IDL. In this way, the
validation library knows where to look for the implementation of the custom
validation:

.. code:: thrift

    #@namespace scala com.twitter.scrooge_internal.thrift_validator
    #@validator com.twitter.scrooge_internal.thrift_validation.example.CustomValidatorExample

    struct CustomValidationStruct {
      1: string email (validation.startWithA = "")
    }

Specify path to custom validators 
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
For Twitter internal applications built with Bazel or Pants, please specify 
the directory for the BUILD file of the custom validator as `validators` 
when defining a `java_thrift_library` target or a `create_thrift_libraries` 
target:

.. code:: python

    java_thrift_library(
        name = "base",
        sources = ["custom_validations.thrift"],
        compiler = "scrooge",
        language = "scala",
        tags = ["bazel-compatible"],
        validators = ["scrooge-internal/src/main/scala/com/twitter/scrooge_internal/thrift_validation/example"],
    )

.. code:: python

    create_thrift_libraries(
        base_name = "thrift",
        sources = ["custom_validations.thrift"],
        generate_languages = [
          "java",
          "scala"
        ],
        tags = ["bazel-compatible"],
        validators = ["scrooge-internal/src/main/scala/com/twitter/scrooge_internal/thrift_validation/example"],
    )

For open source applications built with Pants, please specify the directory 
for the BUILD file of the custom validator as `dependencies` when defining a 
`java_thrift_library` target:

.. code:: python

    java_thrift_library(
        name = "base",
        sources = ["custom_validations.thrift"],
        compiler = "scrooge",
        language = "scala",
        tags = ["bazel-compatible"],
        dependencies = ["scrooge-internal/src/main/scala/com/twitter/scrooge_internal/thrift_validation/example"],
    )

Validation violation reporting
------------------------------

Once Thrift Validation is enabled, the server will run all validations when a
Thrift request comes in, and throw a `ThriftValidationException <https://github.com/twitter/scrooge/blob/b9bc55099d0764bf6061b91c86bc006c33510b1d/scrooge-thrift-validation/src/main/scala/com/twitter/scrooge/thrift_validation/ThriftValidationException.scala>`_
with all validation violations if any validation fails. The client, however,
will receive a `org.apache.thrift.TApplicationException`. This is because
Finagle Thrift clients operate at byte level so they convert any application
exceptions into `TApplicationException` in order to transfer the exceptions
over the wire.

The validation violations are reported via logging, stats, and Zipkin tracing
on both client and server side.

Stats
~~~~~

**thrift_validation/violation/<method_name>/<request_class_name>**
  A server side counter of the total number of failed requests of class
  `<request_class_name>` to the endpoint `method_name`.

**client/<method_name>/failures/org.apache.thrift.TApplicationException**
  A client side counter of the total number of requests that failed with a
  `TApplicationException`, this includes the requests that failed Thrift
  Validation.

Zipkin Tracing
~~~~~~~~~~~~~~

**validation/endpoint**
  Annotates the method name where the invalid request was trying to call.

**validation/request**
  Annotates the invalid Thrift request class name.

Alternative `MethodPerEndpoint` API
-----------------------------------

If you don’t want the server to throw an exception upon receiving an invalid
request, you can create a `MethodPerEndpoint` client by extending
`<your_service_name>.ServerValidationMixin <https://github.com/twitter/scrooge/blob/dd10a0efee67aff81c38c0b6407c3ac8e8cf8a10/scrooge-generator-tests/src/test/resources/gold_file_output_scala/com/twitter/scrooge/test/gold/thriftscala/GoldService%24FinagleService.scala#L23-L44>`_
trait (which extends `MethodPerEndpoint`) and implementing the
`violationReturning<method_name>` API:

.. code:: scala

    val methodPerEndpoint = new ValidationService.ServerValidationMixin {
      override def validate(
        structRequest: ValidationStruct,
        unionRequest: ValidationUnion,
        exceptionRequest: ValidationException
      ): Future[Boolean] = Future.False

      override def violationReturningValidate(
        structRequest: ValidationStruct,
        unionRequest: ValidationUnion,
        exceptionRequest: ValidationException,
        structRequestViolations: Set[ThriftValidationViolation],
        unionRequestViolations: Set[ThriftValidationViolation],
        exceptionRequestViolations: Set[ThriftValidationViolation]
      ): Future[Boolean] = {
        // if any of the request parameters has validation violations, return true, otherwise return false
        if (structRequestViolations.nonEmpty || unionRequestViolations.nonEmpty || exceptionRequestViolations.nonEmpty)
          Future.True
        else Future.False
      }
    }

    val thriftServer =
      Thrift.server
        .serveIface(new InetSocketAddress(InetAddress.getLoopbackAddress, 0), methodPerEndpoint)

    val methodPerEndpointClient = Thrift.client.build[ValidationService.MethodPerEndpoint](
      Name.bound(Address(thriftServer.boundAddress.asInstanceOf[InetSocketAddress])),
      "client"
    )

The Thrift request to the RPC method `<method_name>` is already validated by
the Thrift Validation library, and the violation is returned as parameter
`<request_name>Violations` in the signature of the
`violationReturning<method_name>` API. You could access the violations and
consume them in any forms your application desires.

To override any inherited methods, please extend both `<parent_service_name>.ServerValidationMixin`
and `<child_service_name>.ServerValidationMixin`, then implement the desired `violationReturning<method_name>` methods.

.. note::

    When the `violationReturning<method_name>` is implemented, the server
    will execute the implementation of the `violationReturning` version instead
    of the implementation of the `<method_name>` when the method `<method_name>`
    is called. If you extend the `ServerValidationMixin` without implementing the
    `violationReturning` method, the `<method_name>` implementation will be
    executed when the method is called, and the server will throw a
    `ThriftValidationException <https://github.com/twitter/scrooge/blob/b9bc55099d0764bf6061b91c86bc006c33510b1d/scrooge-thrift-validation/src/main/scala/com/twitter/scrooge/thrift_validation/ThriftValidationException.scala>`_
    per failed validation.

.. note::

    No stats, logging, and tracing will be reported if no
    `ThriftValidationException` is thrown.

Helper validation method
------------------------

When Thrift Validation is enabled, Scrooge will provide a helper method in
the generated class where a validation annotation is applied. For example,
by annotating the struct with:

.. code:: thrift

    struct ValidationStruct {
      1: string stringField (validation.length.min = "6", validation.email = "")
      2: i32 intField (validation.positiveOrZero = "")
      3: i64 longField (validation.max = "100")
      4: i16 shortField (validation.negative = "")
      5: i8 byteField (validation.positive = "")
      6: map<string, string> mapField (validation.size.max = "1")
      7: bool boolField (validation.assertTrue = "")
      8: required string requiredField
      9: optional string optionalField
    }

Scrooge will create a `validateInstanceValue <https://github.com/twitter/scrooge/blob/dd10a0efee67aff81c38c0b6407c3ac8e8cf8a10/scrooge-generator-tests/src/test/resources/gold_file_output_scala/com/twitter/scrooge/test/gold/thriftscala/Request.scala#L444-L467>`_
method in the generated `ValidationStruct` object. You can call the method
anywhere in your code if you need a helper method without creating a RPC call:

.. code:: scala

    val invalidStruct =
      ValidationStruct(
        "email",
        -1,
        101,
        0,
        0,
        Map("1" -> "1", "2" -> "2"),
        boolField = false,
        "anything",
        Some("nothing"))

    val validationViolations = ValidationStruct.validateInstanceValue(invalidStruct)
