Adaptive Scrooge
================

Adaptive Scrooge aims to reduce deserialization costs for the case
where only a small subset of fields of a Thrift object are accessed.

AdaptiveScrooge has an initial recording phase in which it observes
how fields are accessed. At the end of recording phase it generates
a customized decoder and a customized representation. The customized
decoder is optimized to skip all the fields that are not accessed.
It is fast because it doesn’t need to create objects for the
fields it skips. It can skip entire tree of objects. The customized
representation doesn’t allocate any memory for the unused fields
thus reducing memory consumption. It still implements the same
interface, falling back to decoding the entire Thrift in case an
unused field ends up getting accessed. 

If all the fields in a Thrift are accessed then adaptive decoding
is completely turned off, falling back to eager decoding.

AdaptTProtocol
--------------

A special protocol is used to facilitate adaptive decoding. It
builds on top of LazyTProtocol, providing methods that facilitate
adaptive decoding.

### Skip Methods
A number of methods are supplied for skipping fields. They are used
for skipping bytes of unused fields while parsing.

### withBytes
In case a field we considered unused ends up getting accessed we
fallback to decoding the entire Thrift from bytes. To do this we
need a new protocol object which this method provides.

### AdaptContext
AdaptContext is mainly used for building the adaptive decoder. The
generated code and AdaptContext interact delicately to facilitate
adaptive decoding. The delicateness arises from the need to plug
generated code into adaptive decoding. AccessRecordingWrapper, lazy
decoding logic etc live in generated code and they are plugged into
adaptive decoder generation using the buildDecoder method.

AdaptContext has another important method called shouldReloadDecoder.
Protocol is that Adaptive Decoder will be generated once initially
and afterwards only when shouldReloadDecoder returns true. This is
a way of retriggering adaptation. This is critical for writing unit
tests.  In future we might use this mechanism to retrigger adaptation
to adjust to changes in access pattern.

AccessRecordingWrapper
----------------------

AccessRecordingWrapper is a simple wrapper around the regular
Thrift object.  It decorates field accessors to record accesses.
This is how Adaptive Scrooge learns which fields are actively
accessed and which are not. A subtle point about AccessRecordingWrapper
is that the write method needs to use the underlying bytes and not
access all the fields individually. Otherwise simply converting
Thrift object to bytes would end up accessing all fields and adaptive
optimization wouldn't trigger.

AdaptiveDecoder
---------------
   
Adaptive Decoder is a Thrift decoder that is annotated to guide
modifications to it using ASM library at runtime. For every field
it has four special methods:

```
  {{_fieldName}}UsedStartMarker
  {{_fieldName}}UsedEndMarker
  {{_fieldName}}UnusedStartMarker
  {{_fieldName}}UnusedEndMarker
```
Decoder code has sections for both when a field is considered
accessed and when not. The sections are annotated with above marker
functions.

After the recording period finishes AdaptiveDecoder class is modified.
The markers are removed and for every field only one of the two
sections is kept. The used section is similar to how the field would
have been decoded eagerly. The unused section on the other hand is
optimized to skip. The modified class is loaded at runtime using a
special ClassLoader.

In the current implementation adaptation happens only once, based on
the assumption that access pattern will remain the same. If access
pattern changes during the lifetime of process then it's possible
that unused considered fields start getting accessed which would lead
to double decoding and is costly. A restart would fix that. This
limitation would likely be fixed in future.

The decoder class is named {{StructName}}$$AdaptDecoder. The use
of two dollars is to avoid conflicts with regular code and is a
namespacing mechanism. Nested classes are complicated to reload so
the implentation is kept out of the companion object of the struct
on purpose.

Adapted Representation
----------------------

Called {{StructName}}$$Adapt is the template for optimized
representation. At runtime this class is modified as follows:

For unused fields the member variables for direct storage are
removed. This reduces memory footprint. Instead accessor methods
are modified to fallback to decoding the entire Thrift object from
bytes. This is expensive but should be rare.

For used fields accessors are similar to eager Thrift, returning
the stored value in member variable.

The adapted representation is loaded at runtime.


