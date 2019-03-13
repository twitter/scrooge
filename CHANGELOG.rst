.. Author notes: this file is formatted with restructured text
  (http://docutils.sourceforge.net/docs/user/rst/quickstart.html)
  as it is included in Scrooge's user's guide.

Note that ``PHAB_ID=#`` and ``RB_ID=#`` correspond to associated messages in commits.

Unreleased
----------

19.3.0
------

No Changes

19.2.0
------

No Changes

19.1.0
------

* Update asm, cglib, jmock dependencies ``PHAB_ID=D250175``

* scrooge-generator: Add an `immutable` argument to `Generator#genType` that makes it easier to use
  immutable types when generating constants. ``PHAB_ID=D270863``

18.12.0
-------

* scrooge-generator: Set a `LocalContext` value with the current Thrift method in the
  generated Java and Scala code such that the application `Service[-R, +R]` being executed has
  access to information about the current Thrift method being invoked. ``PHAB_ID=D241295``

18.11.0
-------

* scrooge-generator: Allow for `Filter.TypeAgnostic` filters to be applied to a generated
  Java `Service` via a new constructor that takes an additional argument of a `Filter.TypeAgnostic`.
  This filter is then applied per generated method service. ``PHAB_ID=D235709``

18.10.0
-------

* scrooge: Add type annotations to public members in generated code.
  ``PHAB_ID=D229710``

18.9.1
------

* scrooge: Finally remove `maven.twttr.com` as a dependency or plugin repository. With
  the update to a more recent libthrift dependency, this should no longer be necessary.
  ``PHAB_ID=D219665``

18.9.0
------

New Features
~~~~~~~~~~~~

* scrooge-generator: Scala and Java generated Thrift exceptions now
  implement `c.t.f.FailureFlags`. This allows exceptions to carry
  Finagle metadata such as non-retryable. ``PHAB_ID=D204132``

18.8.0
------

* scrooge-core: Add interface for Scala generated Enum objects. ``PHAB_ID=D197147``

* scrooge-core: Trait `c.t.scrooge.ThriftService` is now `c.t.finagle.thrift.ThriftServiceMarker`.
  Scrooge generated service objects now all inherit from `c.t.finagle.thrift.ThriftService`. Also,
  the `AsClosableMethodName` string was formerly part of `c.t.finagle.thrift.ThriftService`, but
  now is defined in the c.t.scrooge package object.
  ``PHAB_ID=D180341``

* scrooge-generator: Thrift service objects now contain `unsafeBuildFromMethods`, which constructs
  a `ReqRepServicePerEndpoint` from a map of
  `ThriftMethod -> ThriftMethod.ReqRepServicePerEndpointServiceType`. It is unsafe because the
  types are not checked upon service construction, only when a request is attempted.
  ``PHAB_ID=D180341``

18.7.0
------

* scrooge-adaptive: Turn the scrooge-adaptive back on as default in ScroogeRunner. `PHAB_ID=D187772``

18.6.0
------

No Changes

18.5.0
------
* scrooge-generator: Add support for construction_required fields in cocoa. ``PHAB_ID=D163127``
* scrooge-generator: Add cocoa initializer for each field in union. ``PHAB_ID=D156591``
* scrooge-generator: Add support for empty struct in cocoa. ``PHAB_ID=D156539``
* scrooge-generator: Fix setter bug for non-primitive type in cocoa. ``PHAB_ID=D156605``

* scrooge-adaptive: Turn the scrooge-adaptive off as default in ScroogeRunner due to
  incompatibility with sbt > 1.0.2. ``PHAB_ID=D163144``

18.4.0
------

* scrooge-generator: Add support for construction_required fields. Add a validateNewInstance method
  to all generated scala companion objects. ``PHAB_ID=D148841``

* scrooge-core: Check for corruption in size meta field of container and throw
  an exception if size is found corrupted. ``PHAB_ID=D150057``
* scrooge: Upgrade libthrift to 0.10.0. ``PHAB_ID=D124620``


18.3.0
------

* scrooge-generator: Add support for mutually recursive structs. ``PHAB_ID=D134470``

18.2.0
------

* scrooge-generator: Add `asClosable` method to `ServicePerEndpoint` and
  `ReqRepServicePerEndpoint` interfaces as well. ``PHAB_ID=D134171``

* scrooge-generator: Remove unused `functionToService` and `serviceToFunction`
  methods along with `ServiceType` and `ReqRepServiceType` type aliases in
  order to simplify code generation.

  NOTE: This functionality can be manually replicated by users if/when needed
  to convert between a Function1 and a Finagle `Service`. ``PHAB_ID=D132171``

* scrooge-generator: Scala generated client now has a asClosable method returns c.t.u.Closable,
  client now can be closed by calling `client.asClosable.close`. Note that `asClosable` won't be
  generated if it is also defined by the user. ``PHAB_ID=D129645``

* scrooge-generator: Renamed subclasses of `com.twitter.scrooge.RichResponse`:
  `ProtocolExceptionResponse`, `SuccessfulResponse`, and `ThriftExceptionResponse`.
  These case classes are for representing different response types and should be only
  used by the generated code. ``PHAB_ID=D132202``

18.1.0
------

* scrooge-generator: Update `c.t.fingale.thrit.service.MethodPerEndpointBuilder`
  to build `MethodPerEndpoint` types. Add new `ThriftServiceBuilder` for
  building the higher-kinded form from a `ServicePerEndpoint`. Users should
  prefer using the `MethodPerEndpointBuilder`. ``PHAB_ID=D127538``

* scrooge-generator: Add more metadata to generated java objects ``PHAB_ID=D122997``
  Includes:

  * struct and field annotations from the idl files
  * which fields have default values
  * which field values of TType.STRING are actually binary fields

* scrooge: Add support for `scrooge.Request` and `scrooge.Response`
  types in generated `ThriftMethod` code. ``PHAB_ID=D122767``

17.12.0
-------

* scrooge: Introduce `scrooge.Request` and `scrooge.Response` envelopes which
  are used in `ReqRepServicePerEndpoint` interfaces and associated code. The
  scrooge `Request` and `Response` allow for passing "header" information (via
  ThriftMux Message contexts) between clients and servers. For instance, a
  server can implement a `ReqRepServicePerEndpoint`, and set response headers
  along with a method response, e.g.,

.. code-block:: scala

   class MyService extends MyService.ReqRepServicePerEndpoint {

     def foo: Service[Request[Foo.Args], Response[Foo.SuccessType]] = {
       Service.mk[Request[Foo.Args], Response[Foo.SuccessType]] { request: Request[Foo.Args] =>
         val result = ... // computations
         Future
           .value(
             Response(
               headers = Map("myservice.foo.header" -> Seq(Buf.Utf8("value1"))),
               result)
       }
     }
   }

  This `ServicePerEndpoint` can then be served using `ThriftMux`:

.. code-block:: scala

   ThriftMux.server.serveIface(":9999", new MyService().toThriftService)

  These response headers will be transported as `Mux#contexts` to the client. If
  the client is using the client-side `ReqRepServicePerEndpoint` it will be able
  to read the headers from the returned `Response` directly. E.g.,

.. code-block:: scala

   val client = ThriftMux.client.reqRepServicePerEndpoint[MyService.ReqRepServicePerEndpoint]

   val response: Response[Foo.SuccessType] = Await.result(client.foo(..))

   if (response.headers.contains("myservice.foo.header")) {
     ...

  Users can also choose to wrap the `ReqRepServicePerEndpoint` with a `MethodPerEndpoint`
  via `ThriftMux.client.reqRepMethodPerEndpoint(reqRepServicePerEndpoint)` in order to
  deal with methods instead of services. See the scrooge documentation for more information.
  ``PHAB_ID=D107397``

17.11.0
-------

* scrooge-generator: Deprecated some scala generated classes and use new ones

  * `FutureIface`         -> `MethodPerEndpoint`,
  * `MethodIface`         -> `MethodPerEndpoint.apply()`,
  * `MethodIfaceBuilder`  -> `MethodPerEndpointBuilder`,
  * `BaseServiceIface`    -> `ServicePerEndpoint`,
  * `ServiceIface`        -> `ServicePerEndpoint`,
  * `ServiceIfaceBuilder` -> `ServicePerEndpointBuilder`.

  To construct a client use `c.t.f.ThriftRichClient.servicePerEndpoint` instead of
  `newServiceIface`, to convert `ServicePerEndpoint` to `MethodPerEndpoint` use
  `c.t.f.ThriftRichClient.methodPerEndpoint` instead of `newMethodIface`. ``PHAB_ID=D105791``

* scrooge-generator: (BREAKING API CHANGE) Change the java generator to no longer
  generate files with `org.slf4j` imports and remove limited usage of `org.slf4j`
  Logger in generated services. ``PHAB_ID=D108113``


17.10.0
-------

* From now on, release versions will be based on release date in the format of
  YY.MM.x where x is a patch number. ``PHAB_ID=D101244``

* scrooge-generator: For generated scala $FinagleService, moved per-endpoint statsFilter to the
  outermost of filter chain so it can capture all exceptions, added per-endpoint response
  classification in statsFilter. ``PHAB_ID=D100649``

* scrooge-generator: Generated scala $FinagleClient takes a `RichClientParam` for all
  configuration params, such as `TProtocolFactory`, `ResponseClassifier`, `maxReusableBufferSize`,
  and `StatsReceiver`, $FinagleService takes a `RichServerParam`. ``PHAB_ID=D83190``

* scrooge-sbt-plugin: Renamed ScroogeSBT.thriftConfig to ScroogeSBT.ThriftConfig for
  sbt 1.0.0.  ``PHAB_ID=D101910``

4.20.0
------

No Changes

4.19.0
------
* scrooge-generator: Generated scala/java code now is using `serviceMap` instead of `functionMap`
  for Finagle services' method implementation. ``PHAB_ID=D73619`` for scala and
  ``PHAB_ID=D76129`` for java

* scrooge-generator: Generated Java code now is using `c.t.s.TReusableBuffer` to reduce
  object allocations. This in turn adds `scrooge-core` as dependency for generated
  java code. ``PHAB_ID=D60406``

* scrooge-generator: support for thrift struct field doccomments for scala
  generated code ``RB_ID=918179``

* scrooge-generator: The `MethodIface` in generated Scala code implements
  `FutureIface`. It already "was" that type in practice but did not implement
  that trait. ``PHAB_ID=D67289``

* scrooge-generator: Generated Cocoa code now supports modular frameworks and
  removes some compiler warnings about implicit casts. ``PHAB_ID=D74200``

4.18.0
------
* scrooge-generator: Expose some methods of TemplateGenerator as static methods ``PHAB_ID=D60494``

* scrooge-generator-tests: Add ability for langauge implementations outside of scrooge directory
  to use GoldFileTest, expose generated files to subclasses of GoldFileTest, add option to keep
  generated files for debugging. ``PHAB_ID=D60494``

* scrooge-adaptive: Add support for adaptive decoding, that learns
  from field access patterns and optimizes the decoder to cheaply
  skip over unused fields. ``RB_ID=908416``

* scrooge-generator: Scala code generation support for annotations on enums ``RB_ID=917467``

4.17.0
------

* scrooge-core: To reduce object allocations, `c.t.s.TFieldBlob` now uses `c.t.io.Buf`,
  and add `c.t.s.TReusableBuffer` for providing thread-safe reusable buffer. ``RB_ID=914874``

* scrooge-core: Add dependency on util-core. ``RB_ID=914874``

4.16.0
------

No Changes

4.15.0
------

* scrooge-core: `c.t.s.ThriftUnion` adds methods `containedValue` and
  `unionStructFieldInfo`. These were already a part of the generated Scala
  implementations for unions, but now it is defined on the trait. ``RB_ID=909576``

* scrooge-core: Removed `c.t.s.ThriftStructCodec` deprecated `encoder`
  and `decoder` methods. Use `encode` and `decode` instead. ``RB_ID=909714``

* scrooge-core: Remove deprecated `encoder` and `decoder` methods
  from `c.t.s.ThriftStructCodec`. Use `encode` and `decode` instead.
  ``RB_ID=909714``

* scrooge-generator: Add parsing, AST, and Scala code generation
  support for annotations on enums, enum fields, services, and
  service methods. ``RB_ID=908556``
* scrooge-generator: Fix default values for collections in scala bindings ``RB_ID=908152``
* scrooge-generator: MethodIfaceBuilder#newMethodIface now returns
  a MethodIface. ``RB_ID=907700``

* scrooge-generator: Scala's types for ServiceIfaces are now a `Service` from
  `ThriftMethod.Args` to `ThriftMethod.SuccessType`, instead of `ThriftMethod.Args`
  to `ThriftMethod.Result`. This is a breaking API change though it should generally
  be easy to adapt existing code to it. ``RB_ID=908846``

4.14.0
------

No Changes

4.13.0
------

* scrooge-linter: Add thrift definition linter warnings if generated
  thrift will exceed JVM HotSpot ClipInlining check.
  ``RB_ID=896379``

4.12.0
------

* scrooge-generator: Remove check for 22 args when generating scala
  ServiceIface. Now that we no longer support Scala 2.10 we can always
  generate a case class for the generated scala ServiceIface.
  ``RB_ID=882203``
* scrooge-generator: Don't allow Structs and Typedefs with the same
  identifier. Structs and typedefs should not have the same name. This
  makes it difficult to properly support self-referencing types.
  ``RB_ID=881684``
* scrooge-generator: Fix pathological case for self-referencing
  types with Java generation. ``RB_ID=880813``

4.11.0
------

* scrooge-generator: Add support for self-referencing types from
  `pinsri` via https://github.com/twitter/scrooge/pull/244
  ``RB_ID=873802``

* scrooge: Remove unmaintained bin/ directory. ``RB_ID=873411``

4.10.0
------

No Changes

4.9.0
------

* scrooge-core: `c.t.scrooge.TReusableMemoryTransport` now uses TUnboundedByteArrayOutputStream
  instead of TByteArrayOutputStream to avoid buffer reallocation on reset.

4.8.0
------

Breaking API Changes
~~~~~~~~~~~~~~~~~~~~

* scrooge-sbt-plugin: Allow scrooge to build bindings for more than one
  language. To reflect this, `ScroogeSBT.autoImport.scroogeLanguage` has been
  renamed to `scroogeLanguages` and is now a `SettingKey[Seq[String]]`
  instead of a `SettingKey[String].` ``RB_ID=846198``

* Builds are now only for Java 8 and Scala 2.11. See the
  `blog post <https://finagle.github.io/blog/2016/04/20/scala-210-and-java7/>`_
  for details. ``RB_ID=828898``

4.7.0
-----

* scrooge-core, scrooge-generator: `c.t.scrooge.ThriftEnum` now includes an
  `originalName` method which represents the name as defined in the Thrift
  IDL file. ``RB_ID=820075``

4.6.0
-----

* scrooge-ostrich: Removed scrooge-ostrich module
* scrooge-runtime: Deleted unnecessary scrooge-runtime module
* scrooge-generator: Remove broken experimental-java generator.

4.5.0
-----

* scrooge: Improve implementation of service#FunctionType

4.4.0
-----

* NOT RELEASED

4.3.0
-----

* scrooge: Rename __ServiceIface to BaseServiceIface
* scrooge: Add methods for converting between function and service implementations of ThriftMethods.

4.2.0
-----

* bump finagle version to 6.30

4.1.0
-----

* bump finagle version to 6.29

4.0.0
-----

* scrooge: Scrooge 4.0.0 includes backward compatibility patches for Finagle service per endpoint generation. This allows using Thrift endpoints as Finagle Services and combining them with Filters.

3.x
-----

3.20.0
------

* scrooge: Generate a finagle Service per thrift method (Service interface)

3.19.0
------
* scrooge: Performance improvements and bug fixes.
* scrooge-sbt-plugin: Add output language support in scrooge-sbt-plugin.

3.18.1
------
* scrooge-maven-plugin: Fix bug with plugin parameters.

3.18.0
------
* scrooge: Support ignoring unknown enum ids.
* scrooge: Output full exception chain in client stats.
* scrooge: Add union metadata to generated scala code.
* scrooge-maven-plugin: Resolve IDLs transitively; deprecate the dependencyIncludes option.
* scrooge-sbt-plugin: Add thrift files to published artifact in sbt-plugin.
* scrooge-sbt-plugin: Upgrade to autoPlugin.

3.17.0
------

* scrooge: add is required to ThriftStructFieldInfo.
* scrooge minor docs update: add logo and short description.
* scrooge-serializer: Remove dependency on scrooge-runtime.
* scrooge: Cache mustache resources to improve generation performance.
* scrooge: Disallow identifiers that are thrift keywords.
* scrooge: Remove SafeVarargs for JDK 6 compatibility.

3.16.6
------

* scrooge-core: Added scala 2.11 support
* scrooge-core: scrooge: add .withoutPassthrough method that recursively removes passthrough fields
* scrooge-doc: Fix formatting in the CLI help page.
* scrooge-linter: Cleaner logging and options.
* scrooge-linter: remove invalid CONFIG.ini.
* scrooge: prefer Protocols.binaryFactory over TBinaryProtocol.Factory

3.16.3
------

* scrooge-core: Add union metadata for reflection
* scrooge-doc: Clarify docs on CLI usage
* scrooge-generator: Fix error message for missing required field
* scrooge-generator: Modify compiler to accept a Scaladoc comment at the end of Thrift file
* scrooge-generator: Normalize scalatest versions between poms and 3rdparty
* scrooge-generator: Stricter checks for invalid Thrift filenames
* scrooge-ostrich: Default to using `Protocols.binaryFactory`

3.16.1
------

* release finagle v6.18.0
* release util v6.18.0
* scrooge-linter: Fix multiple arguments to linter + pants/mvn fixes
* scrooge: Separate flow for linter
* scrooge: Skip includes when linting

3.16.0
------

* Upgrade dependencies to latest versions
* scrooge: Move scrooge-linter into scrooge
* scrooge: Add SimpleID.originalName for enum fields.

3.15.0
------

* scrooge: Bumping finagle to 6.16.0
* scrooge: Bump util to 6.16.1-SNAPSHOT

3.14.1
------

* scrooge-generator: Allow union field names to match struct names

3.14.0
------

* scrooge: Use scala.Option in all com.twitter.scrooge files to avoid conflict with com.twitter.scrooge.Option
* scrooge: Allow for Longs as const values
* scrooge: Make mustache parser threadsafe
* scrooge: Removing scrooge-generated null checks for primitive Scala types
* scrooge-ostrich: Add a flag for enabling ThriftMux

3.13.2
------

* scrooge: bump finagle + util versions

3.13.1
------

* scrooge-generator: Use OutputSreamWriter to write non ascii characters correctly.

3.13.0
------

* scrooge: add sbt 0.13 variant of scrooge-sbt-plugin
* scrooge: Add scrooge/scrooge-generator/BUILD
* scrooge: enable structs for the RHS of consts in scala
* scrooge: handle all shapes of RHS structs
* scrooge: scrooge: expose IDL annotations in generated structs
* scrooge: scrooge: throw an error when reading a field with the wrong type
* scrooge: Test uses of scala.Product are fully qualified
* scrooge: Thrift structs with fields named "n" can't use productElement to get that field
* scrooge: upgrade finagle to 6.13.1
* scrooge: upgrade util to 6.13.2

3.12.3
------

* scrooge: add the thrift root to the list of includes for scrooge
* scrooge: Automatically whitelist all idl jar dependencies
* scrooge: fixed issue when default value is enum from other namespace where namespace is missing in generated code
* scrooge: Update mustache to 0.8.13
* scrooge: update util to 6.12.0
* scrooge: update finagle to 6.12.1
* scrooge: update util to 6.12.1
* scrooge: add extra fields to generated companion object for reflection use
* scrooge: capture unknown union values as its own value (THRIFT-99)
* scrooge: Update scrooge to remove date from Generated annotation so generated code is reproducible.

3.12.2
------

* scrooge: Added missing writeFieldEnd() for passthrough fields
* scrooge: Bump finagle to 6.11.1
* scrooge: Bump util to 6.11.1
* scrooge: WriteFieldStop during transfer

3.12.1
------

* scrooge: properly handle field annotations

3.12.0
------

* scrooge: use a TReusableMemoryTransport in finagle services
* Bump guava to 15.0
* scrooge-generator: trim some allocations from generated scala code
* scrooge: use scalatest, remove specs
* added scala namespace to demo
* Rm all imports of `scala.Some`

3.11.2
------

* scrooge: bump finagle to 6.10.1-SNAPSHOT, util to 6.10.1-SNAPSHOT
* scrooge-generator: Attach thrift annotations to generated AST
* scrooge-generator: Ensure enums with values of the same name will compile
* scrooge-maven-plugin: Overwrite and warn if the current file is older

3.11.1
------

* scrooge: bump finagle version to 6.8.0
* scrooge: bump util version to 6.8.0
* scrooge: secondary struct class constructors without _passthroughFields for backwards compatibility
* scrooge-generator: fixed comment parsing bug

3.11.0
------

* scrooge-generator: simplify synthesized structs For synthesized service method arg and results structs
* scrooge-generator: special, scrooge-only syntax for scala namespace
* scrooge-generator: don't backquote scala identifiers in Enum.valueOf string constants

3.10.2
------

* scrooge-generator: produce slimmer code, remove _passthroughFields from object apply method

3.10.1
------

* scrooge-generator: default passthrough value, valid method names.

3.10.0
------

* scrooge-serializer: simpler BinaryThriftStructSerializer builder
* scrooge-maven-plugin: check for null from Artifact.getDependencyTrail

3.9.2
-----

* scrooge: support larger structs
* scrooge: allow oneway
* scrooge: always generate passthrough code

3.9.1
-----

* scrooge-generator: remove deprecation warnings removed deprecation warnings for FutureIface, FinagledClient, and FinagledServer. Since Jeff is working on finagle-free code generation, there is no good reason to push people off of these classes onto the replacements I added, only to deprecate those classes in the near future.
* scrooge-generator: fixed imports for union

3.9.0
-----

* scrooge use scala option in metadata
* provide type parameters in metadata
* automatically whitelist all idl jar dependencies
* fix scrooge build properties
* bump finagle to 6.6.3-SNAPSHOT
* scrooge: support backslash escapes
* bump poms to finagle 6.6.1-SNAPSHOT
* bump util to 6.5.1-SNAPSHOT

3.8.0
-----

* scrooge: passthrough field improvements
* Scrooge doesn't title case extended services properly
* update scrooge demo
* fix test breakage on sbt

3.7.0
-----

* scrooge-serializer: tighten up dependencies scrooge-serializer only needs to depend on scrooge-core, not scrooge-runtime (the pants BUILD file already did this).
* We think that mustache actually handles the escaping so that this additional escaping is not needed.
* bump util to 6.4.1-SNAPSHOT
* scrooge-runtime => scrooge-core
* properly qualify service parents
* scrooge-ostrich: add thriftProtocolFactory as val The generated ThriftServer class has a thriftProtocolFactory field that some subclasses use.
* scrooge: removed ostrich generation.
* remove use of deprecated generated ostrich ThriftServer

3.6.0
-----

* scrooge-generator: fixed whitespace eating in strings ThriftParser extends RegexParsers.
* scrooge-generator: Fixup java codegen issues surfaced by converting ads:ad-review-tests in science to scrooge.
* scrooge-generator: add support for scala namepsace
* scrooge-generator: Need to filter out items that are not set when rendering default struct values.
* scrooge-maven-plugin: Make scrooge plugin find thrift files in idls when run only with reactor projects in a clean env
* scrooge-generator: Allow default struct values in the java generator.
* scrooge-ostrich: search harder for FutureIface
* scrooge-generator: rename Service$ThriftServer to Service$OstrichThriftServer - fixes breakage under scala 2.10 - also removed ostrichService.java which wasn't used
* scrooge-generator: allow trailing comma at the end of a map

3.5.0
-----

* scrooge: breaking out finagle, higher-kinded-type interface
* use apply instead of cons for enum list all
* Cleanup around the TypeResolver
* update ostrich related docs
* scrooge-ostrich This review introduces a new, temporary scrooge subproject, which is intended to help in the migration away from generating ostrich code in scrooge.

3.4.0
-----

* BREAKING: remove list generation from enums (was causing compile errors. will revisit)
* move TypeResolver and ParseException into the frontend package
* create scrooge-core leaving legacy finagle code in scrooge-runtime. (scrooge-runtime will be deprecated soon)
* treat non-letters as case-less
* update docs and release process for twitter-server, scrooge
* BREAKING: move serializer into its own project
* keep the order of the values in the constant map in the parser. Should be a no-op for scala that converts it to a map in the generator
* Remove the tracerFactory usage and use tracer instead.
* create scrooge documentation site
* tiny fix for oneway support

3.3.2
-----

* bugfix: collections of enums now identify as i32 on the wire

3.3.1
-----

* provide a mechanism for dynamicallly pluggable backends
* make enum list of values lazy
* remove the include mapping hack
* fix maven plugin references includes

3.3.0
-----

* Documenation fixes
* fix ThriftStructMetaData use camelCase method names to match generated
  code
* maven-plugin: skip file copy from references if existing file is the
  same
* POTENTIALLY BREAKING CHANGES:
* Identify enum fields as TType.ENUM but maintain backward
  compatibility by identifying them as I32 on the wire
* maven-plugin - do not extract dependencies into their own
  subdirectories

3.2.1
-----

* add list method to enums that lists all values
* bugfix: ThriftUtil was not being imported for services
* add ability to attach additional passthrough fields

3.2.0
-----

* BREAKING CHANGE:
  Make java gen experimental. There are changes coming down the pipe that
  will dramatically refactor java's codegen.

3.1.10
------

* do not use an intermedial `val` for passthroughs. Eliminates the possibility
  of a name collision
* eliminate the possibility of namespace collision for "runtime"
* revert identification of Enums and TType.ENUM (back to I32)
* eliminate all use of ThriftUtil unless it's needed

3.1.9
-----

* bump to util-6.3.6 / finagle-6.5.0
* [EXPERIMENTAL] add ability to pass through additional fields
  enable with --enable-passthrough
* Create the ability to map includes to directories to bridge scrooge2 and
  scrooge3 maven layouts
* show the filename of the file being parsed in error messages
* identify enums as TType.ENUM

3.1.8
-----

* generator: thrift idl containing UTF-8 produces
  java.nio.charset.UnmappableCharacterException
* generator: Replace backslash with forward slash in file URI
* sbt-plugin: Include (and optionally compile) external thrift files.
* generator: remove unnecessary apply method for decode (causes issues with
  named args)

3.1.7
-----

* Use explicit version numbers

3.1.6
-----

* Depend on the latest patch version of util/finagle

3.1.5
-----

* add back the --import-path flag as a deprecation step
* add sbt-plugin
* use maven as the build system for the maven plugin

3.1.2
-----

* BREAKING CHANGE: In the maven plugin: change the dependentConfigs param to dependentIncludes

* optimize empty collections on deserialization
* upgrade to finagle 6.4.0 and util 6.3.4

3.1.1
-----

* BREAKING CHANGE:
  We finally made scrooge-runtime to be backward with Scrooge 2. This requires
  a name change for the ThriftStructCodec. From now on, all objects generated
  by Scrooge 3 will use ThriftStructCodec3.
  This will affect you only if your code is using ThriftStructCodec directly,
  which is not common.
* scrooge now releases jar-with-dependencies
* add language option tag to scrooge-maven-plugin, thanks to @eirslett
* some directory reorganization of the demos

3.1.0
-----

* Dependency changes: now on util/finagle 6.3.0
* demo project now shows how to construct finagle server and client using
  generated code
* --ostrich flag implies --finagle flag

3.0.9
-----

* Remove "provided" scope of finagle in scrooge-runtime. So it brings Finagle
  6.1.0 as transit dependency to your project
* Make the generated Scala code backward compatible with Finagle 5. The impact
  to users on Finagle 6 is that you will see a lot of warnings saying that
  tracerFactory is deprecated.

3.0.8
-----

* When scrooge-maven-plugin extracts Thrift files from a dependency artifact, it
  now puts them in a sub folder named after the artifact id. This way, the user
  project can use same-named Thrift files from different artifacts.
* Title case and camel case more consistent with previous version before 3.0.7
  We still preserve consecutive upper cases but not in an all-up-case string, eg:

::

  TBird (original) -> tBird (camel case) -> TBird (title case)
  HTML (original) -> html (camel case) -> Html (title case)

* Thanks to @erikvanoosten - Finagle client can throw exception on void function.
* Thanks to @brancek - Support documentation on enum values.
* Thanks to @erikvanoosten - Reorganizing test folder, and add Apache standard test

3.0.7
-----

* All on-wire names in the Thrift messages are now consistent with
  Apache generated code. This allows Scrooge generated services to exchange
  Thrift messages with Apache generated services.
* Title case ids now preserve consecutive upper case letters. Eg:

::

  TBird (original) -> Tbird (old) -> TBird (now)

  See test case in scrooge-generator/src/test/scala/com/twitter/scrooge/ASTSpec.scala

* scrooge-maven-plugin now enforces an explicit white list in <dependencyConfig>.
  The old behavior is that if a dependency artifact has a "idl" classifier, we
  will extract thrift files from it to compile. The new behavior is that the
  artifact must be explicitly included in <dependencyConfig>. The dependencies
  here include both direct dependencies(specified in project pom file) and
  indirect dependencies (everything in the dependency tree).
* Now supports "scala" as a namespace scope. It is treated same as "java".
* Now supports "*" as a default namespace scope

3.0.6
-----

* Released a scrooge-maven-plugin, for maven projects to integrate Scrooge in
  their pom files. Also released a demo of how to use scrooge-maven-plugin
* scrooge-runtime is now backward compatible with scrooge-runtime 2.X.X. The
  following classes and methods are deprecated:
* FinagleThriftClient
* FinagleThriftService
* ThriftStructCodec.decoder
* ThriftStructCodec.encoder
* scrooge-runtime now can introspect generated ThriftStruct. See the new
  ThriftStructMetaData class.
* BREAKING: in scrooge-runtime, com.twitter.ScroogeOption is now renamed to
  just Option. This is mainly for Java code. But if you need to use it in
  Scala code, make sure to address ambiguity with scala.Option.
* Updated APIs of scrooge-generator. See com.twitter.scrooge.Compiler class
* Fix the stats reporting for the java scrooge thrift code generation

3.0.5
-----

Bug fixes

* Constant definitions now can be of "set" type.
* Fix letter cases of enum fields(Java uses upper case; Scala uses title case)

Dependencies

* Remove dependency on org.scalatest, com.twitter.scalatest (not in Maven
  Central)
* Update dependency of util/finagle/ostrich to 6.1.0
* Project dependencies are all in Maven Central now. You don't need to have
  access to Twitter internal repository anymore.

3.0.4
-----

Features:

* add --dry-run option to parse and validate source thrift files, reports any
  errors, but does not emit any generated source code. It can be used with
  --gen-file-mapping to get the file mapping

Bug fixes

* union types now can contain primitive types.
* constants defined in the same file now can be referenced.

Dependencies

* Update dependency of util/finagle/ostrich to 6.0.6

3.0.3
-----

* Scrooge artifacts now deploys to Maven central via Sonatype
* Scrooge project builds in Travis CI
* Features
* Fully qualifying ids imported by "include" statements. We don't generate
  "import" statements anymore.
* Remove unnecessary finagle jar dependencies for vanilla generated code.
* Add tests
* non-finagle usage; see NonFinagleSpec.scala
* struct immutability and deep copying; see ImmutableStructSpec.scala
* Bug fixes
* move "validate" method from Scala struct trait to object, so that the thrift
  struct can define a "validate" field without name clashing.

3.0.2
-----

* Adding a "--gen-file-map <path>" option to Scrooge command line. It tells
  what output files each input Thrift files generates, in the following format:

::

  inputPath/input.thrift -> outputPath/Constants.scala
  inputPath/input.thrift -> outputPath/FooStruct.scala

* The generated enums now have a common trait ThriftEnum(defined in
  scrooge-runtime), that allows you to query the name as well as the value of
  the enum field.
* The generated Scala enums now are Java-serializable.
* The generated FinagledClient class takes val arguments to make "service",
  "protocol" accessible:

::

  class FinagledClient(
    val service: ...,
    val protocol: ...,
    val serviceName: ...
    stats: ...
  )

3.0.1
-----

Features and bug fixes

* Doc comments are included in the generated code.
* Generated exception structs now have getMessage() method
* Generate header that emits Scrooge version
* You can now import a directory or a Jar/Zip file through command line
  argument, which will be stored in a chain of paths maintained by Scrooge.
  Then refer to a file using relative path in the thrift "include" statement.
  Scrooge will locate the file in the path chain.
* Introduce a "strict" mode that defaults to on. Unfavored syntax throws an
  exception when "strict" mode is on and prints a warning when it's off. The
  strict mode can be disabled by specifying the "--disable-strict" argument.
* The "oneway" modifier is treated as an OnewayNotSupportedException in strict
  mode and a warning in non-strict mode.
* Support Union types. Given:

::

  union Point {
    1: double x
    2: double y
    3: Color color = BLUE
  }

  // Scrooge generates:
  sealed trait Point
  object Point {
    case class X(x: Double) extends Point
    case class Y(y: Double) extends Point
    case class Count(color: Color = Color.Blue) extends Point
  }

  The "required" and "optional" modifiers in a union type will throw
  exceptions in strict mode and print warnings in non-strict mode.

* Have a common trait ThriftException for all the thrift exception structs.
* Support cross file service inheritance. Now you can do
  include "foo.thrift"
  service MyService extends foo.FooService { ... }
* Bug fix: It couldn't resolve a symbol imported through a relative path and
  threw an UndefinedSymbolException
* Bug fix: namespace aliasing put the parentheses in the wrong place.
* Bug fix: services using binary fields wouldn't compile
* Bug fix: cross-file const referencing didn't work

Implementation updates

* Project structure:
* frontend: Importer and ThriftParser
* mustache: everything related to mustache, including template parser, loader
  and handlebar
* ast: Thrift AST definition
* backend: code generation include various generators and dictionaries to
  hydrate Mustache templates.
* Redefine clear and separate responsibilities of each components:
* Move ID manipulation(concatenation, case conversion, keyword rewriting etc)
  to Generator phase.
* Utilizing Scala static type checking to enforce scoping correctness by
  introducing SimpleID and QualifiedID to AST.
* Enforce dictionary key uniqueness for nested Mustache templates.
* Scrooge project is now on Maven
* Delete obsolete code and tests

Dependencies:

* Upgraded to util 5.3.13, finagle 5.3.30
* Removed dependency on sbt
* Add dependency on maven

3.0.0
-----

* Java code generation is now supported!
* Scala code now generates a set of classes for each struct:
* a base trait
* an immutable case class (used as the default implementation)
* a proxy trait (to make it easy to build proxy classes)
* Moved scrooge-runtime into the same repo with scrooge, which is now called
  scrooge-generator. Both projects will keep version numbers in sync now.
* Changed the way required/optional is treated on fields, and default values,
  to more closely match the way Apache Thrift works. (This is described in
  more detail in a new section of the README.)
* Fixed constant sets.
* Fixed thread safety in finagle ThriftServer.
* Fixed the resolution of #include directives that follow relative paths.
* Removed the finagle dependency from scrooge-runtime so that code generated
  with scrooge can be loosely coupled with finagle, or optionally not depend
  on finagle at all (if you don't build finagle bindings).
* Fixed typedef references that were relative to #included files.
* Made various improvements to the internal template system.
* Fixed test speed by using scrooge to generate code that the tests build
  against, avoiding runtime evaluation.

Dependencies:

* Upgraded to thrift 0.8.0, util 4.0, and finagle 4.0.
* Upgraded to sbt 0.11.2.
* Upgraded to scala 2.9.2.

2.5.4
-----

* Addressed an issue where structs with the same name but from different
  namespaces/packages would conflict. Now using a package alias to
  disambiguate.


2.5.3
-----

* Minor bug fix for serviceName name class with.


2.4.0
-----

* added support for structs with more than 22 fields, which previously was the
  limit as that is the max case-class size in scala. For structs larger than
  this, instead of using case-classes, normal classes are used but with most of
  the case-class boilerplate support code also generated, allowing these structs
  to be used as if they were case-classes. The only exception is that there is
  no unapply method; but do you really want to unapply 23+ fields in a match
  statement?


2.3.1
-----

* thriftProtocolFactory in generated ThriftServer now has
  type of TProtocolFactory, so you can override it with other
  protocol factories.


2.3.0
-----

* You can now override serverBuilder in ThriftServer to provide
  additional server configuration
* The protocol factory to the FinagledClient now has a default
  value of TBinaryProtocol.Factory, which means you don't have
  to specify it when using the default.


2.2.0
-----

* tracerFactory support in ThriftServer.

2.1.0
-----

* Support for tracing in server.

2.0.2
-----

* fixes a bug in which namespace mapping was not applied
  recursively to included documents.


2.0.1
-----

* fixes a bug in which qualified service names from imported
  thrift files were not resolved properly.


2.0.0
-----

* fixes a bug with enum in which the first value was wrong.

1.1.1
-----

* scrooge-runtime-1.0.1
* Each thrift struct companion object now extends ThriftStructCodec
* Correctly resolving enum constants and Const values.
* Title-casing enum value names.
* Added support for namespace renaming from the command line.

