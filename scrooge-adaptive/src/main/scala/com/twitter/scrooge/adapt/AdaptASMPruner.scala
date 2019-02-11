package com.twitter.scrooge.adapt

import java.io.InputStream
import java.util.ListIterator
import org.objectweb.asm.tree._
import org.objectweb.asm.{ClassReader, ClassWriter, Opcodes}

private[adapt] object AdaptAsmPruner {
  val UsedStart = "usedStartMarker"
  val UsedEnd = "usedEndMarker"
  val UnusedStart = "unusedStartMarker"
  val UnusedEnd = "unusedEndMarker"

  val MemberPrefix = "m_"
  val SetterPrefix = "set_"
  val DelegatePrefix = "delegated_"
  val DecodeMethodName = "decode"

  /**
   * Prune the Adapt object. We get rid of field members for unused fields. We
   * also rename/remove methods so that unused field accesses result in full
   * deserialization and delegation to thus created object.
   * @param useMap useMap is also used to validate field names, it should have
   *               entries for all fields, ones that are not accessed should have
   *               0 as value.
   *               Important: field name should be in camel case.
   *
   * An example of how the pruning/renaming is done follows.
   * Original scrooge generated Adapt code has something like the following for
   * each field:
   *   private[this] var m_boolField: Boolean = _
   *   def set_boolField(boolField: Boolean) = m_boolField = boolField
   *   def boolField: Boolean = m_boolField
   *   def delegated_boolField: Boolean = delegate.boolField
   *
   * If boolField is unused we remove m_boolField, set_boolField and boolField
   * and rename delegated_boolField to boolField. Overall effect is that access
   * of such a field will be delegated to delegate which will typical then
   * fallback to eagerly deserializing the thrift object from bytes.
   *
   * If boolField is used we remove delegated_boolField since it's not needed.
   *
   * @return Bytes of the pruned Adapt class.
   */
  def pruneAdapt(templateFqdn: String, useMap: Map[String, Boolean]): Array[Byte] = {
    // Using ASM prune Adapt first and then decoder
    val classReader = new ClassReader(classBytesStream(templateFqdn))
    val classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES)
    val classNode = new ClassNode()
    classReader.accept(classNode, ClassReader.EXPAND_FRAMES)

    def isUnusedField(fieldName: String): Boolean =
      useMap.get(fieldName) match {
        case Some(used) => !used
        case None => false
      }

    // Get rid of unused field members
    val fieldIter = classNode.fields.iterator()
    while (fieldIter.hasNext) {
      val fieldNode = fieldIter.next().asInstanceOf[FieldNode]
      val prefixedFieldName = fieldNode.name
      if (prefixedFieldName.startsWith(MemberPrefix)) {
        val fieldName = prefixedFieldName.substring(MemberPrefix.size)
        if (isUnusedField(fieldName)) {
          fieldIter.remove()
        }
      }
    }

    val methodIter = classNode.methods.iterator()
    while (methodIter.hasNext) {
      val methodNode = methodIter.next().asInstanceOf[MethodNode]
      val name = methodNode.name

      // Remove accessor method for unused field, we replace it with renamed
      // delegate method below
      if (isUnusedField(name)) {
        methodIter.remove()
      }

      // Rename or remove delegate methods
      if (name.startsWith(DelegatePrefix)) {
        val fieldName = name.substring(DelegatePrefix.size)
        if (useMap.contains(fieldName)) {
          if (useMap(fieldName)) { // used field, remove delegate function
            methodIter.remove()
          } else { // unused field, rename delegate
            methodNode.name = fieldName
          }
        }
      }

      // Remove setter methods for unused fields
      if (name.startsWith(SetterPrefix)) {
        val fieldName = name.substring(SetterPrefix.size)
        if (isUnusedField(fieldName)) {
          methodIter.remove()
        }
      }
    }

    classNode.accept(classWriter)
    classWriter.toByteArray
  }

  /**
   * Load class bytes using ClassLoader that loaded AdaptAsmPruner. System
   * ClassLoader may not have the class bytes e.g. in case of scala console.
   */
  private[this] def classBytesStream(dottedName: String): InputStream =
    this.getClass.getClassLoader.getResourceAsStream(resourceName(dottedName))

  private[this] def findDecodeMethod(methods: java.util.List[_]): MethodNode = {
    val i = methods.iterator()
    while (i.hasNext) {
      val mn = i.next().asInstanceOf[MethodNode]
      if (mn.name == DecodeMethodName) {
        return mn
      }
    }
    throw new IllegalStateException("decode method not found")
  }

  private[this] def isAdaptTProtocolModuleLoadInstruction(insn: Any): Boolean =
    insn match {
      case f: FieldInsnNode =>
        f.owner == "com/twitter/scrooge/adapt/AdaptTProtocol$" &&
          f.name == "MODULE$"
      case _ => false
    }

  /**
   * Read a marker.
   * Markers are three instructions long and look like:
   * getstatic       #38 // Load AdaptTProtocol
   * bipush/ICONST_* fieldId
   * invokevirtual   #150 // Call AdaptTProcol.markerMethod
   *
   * This method tries to read marker and returns the associated
   * field id if successful.
   * It expects to be at the start of the first instruction of the
   * marker (getStatic). Always returns iter at the same original
   * location, effectively does not mutate iter.
   */
  private[this] def readMarker(iter: ListIterator[_], markerType: String): Option[Short] = {
    // Skip first instruction
    // There should be at least 3 instructions, early exit if none
    if (!iter.hasNext) {
      return None
    }

    // This instruction should be loading AdaptTProtocol$MODULE$, if not
    // then this is not a valid marker, early exit
    if (!isAdaptTProtocolModuleLoadInstruction(iter.next())) {
      iter.previous()
      return None
    }

    // Skip second instruction
    // There should be at least 3 instructions, early exit if only one
    if (!iter.hasNext) {
      // Restore to original state before exiting
      iter.previous()
      return None
    }
    iter.next()

    // There should be at least 3 instructions, early exit if only two
    if (!iter.hasNext) {
      // Restore to original state before exiting
      iter.previous()
      iter.previous()
      return None
    }

    iter.next() match {
      case mInsn: MethodInsnNode if mInsn.name == markerType =>
        iter.previous()
        iter.previous()
        val fieldId = readFieldIdFromInstruction(iter.next())
        iter.previous()
        iter.previous()
        Some(fieldId)
      case _ =>
        iter.previous()
        iter.previous()
        iter.previous()
        None
    }
  }

  private[this] def readFieldIdFromInstruction(insn: Any): Short =
    insn match {
      case i: InsnNode =>
        i.getOpcode match {
          case Opcodes.ICONST_0 => 0
          case Opcodes.ICONST_1 => 1
          case Opcodes.ICONST_2 => 2
          case Opcodes.ICONST_3 => 3
          case Opcodes.ICONST_4 => 4
          case Opcodes.ICONST_5 => 5
          case Opcodes.ICONST_M1 => -1
          case _ =>
            throw new IllegalStateException(
              s"Unexpected opcode ${i.getOpcode} while trying to read fieldId"
            )
        }
      case i: IntInsnNode =>
        i.getOpcode match {
          case Opcodes.BIPUSH => i.operand.toShort
          case Opcodes.SIPUSH => i.operand.toShort
        }
      case _ =>
        throw new IllegalStateException(
          s"Unexpected instruction $insn while trying to read fieldId"
        )
    }

  /**
   * Marker consists of three instructions, remove them.
   */
  private[this] def deleteMarker(i: ListIterator[_]) = {
    i.next()
    i.remove()
    i.next()
    i.remove()
    i.next()
    i.remove()
  }

  /**
   * Iterator should be set at just before the start marker
   * (note marker is 3 instructions).
   */
  private[this] def deleteMarkedSection(
    iter: ListIterator[_],
    markerType: String,
    deleteContent: Boolean
  ): Unit = {
    deleteMarker(iter)
    while (iter.hasNext) {
      readMarker(iter, markerType) match {
        case Some(_) =>
          deleteMarker(iter)
          return
        case _ =>
          iter.next()
          if (deleteContent) {
            iter.remove()
          }
      }
    }
  }

  private[this] def resourceName(dottedName: String): String =
    jvmFqdn(dottedName) + ".class"

  private[this] def jvmFqdn(dottedName: String): String =
    dottedName.replace('.', '/')

  private[this] def processMarkers(di: InsnList, useMap: Map[Short, Boolean]): Unit = {
    val iter = di.iterator
    while (iter.hasNext) {

      /*
       * Each field has sections in the template for when it is used or unused.
       * Based on whether the field is used we keep corresponding section and
       * delete the other.
       */
      // Read the marker for used section
      readMarker(iter, UsedStart) match {
        case Some(fieldId) =>
          deleteMarkedSection(iter, UsedEnd, !useMap(fieldId))
        case _ =>
        // Note we don't move the iterator forward here to check below
        // if this is an unused marker.
      }
      // Read the marker for unused section
      readMarker(iter, UnusedStart) match {
        case Some(fieldId) =>
          deleteMarkedSection(iter, UnusedEnd, useMap(fieldId))
        case _ =>
          // Unlike the used case above, we don't have anything else to
          // check here, so move the iterator forward.
          iter.next()
      }
    }
  }

  /**
   * Decoder code generated by scrooge is instrumented with marker functions
   * that guide on how to modify the code for used and unused fields. The
   * protocol for this is the following. Each field has the following marker
   * methods:
   *   def _fieldNameUsedStartMarker(d: Int) = {}
   *   def _fieldNameUsedEndMarker(d: Int) = {}
   *   def _fieldNameUnusedStartMarker(d: Int) = {}
   *   def _fieldNameUnusedEndMarker(d: Int) = {}
   *
   * These methods take a dummy int parameter to guard against compiler
   * optimizing these no op methods away.
   *
   * Important: field name should be in camel case.
   *
   * All the code for when the field is considered used should be put between the
   * _fieldNameUsedStartMarker and _fieldNameUsedEndMarker method calls. For
   * unused case, code should be but between _fieldNameUnusedStartMarker and
   * _fieldNameUnusedEndMarker.
   *
   * This method removes the unused code sections and the invocations of marker
   * methods.
   *
   * @param decoderTemplateFqdn
   * @param useMap Map indicating whether each field is considered used or
   *               unused. This map should have entries for all the fields.
   *               Important: field name should be in camel case.
   * @return Bytes of the pruned Decoder class.
   */
  def pruneAdaptDecoder(decoderTemplateFqdn: String, useMap: Map[Short, Boolean]): Array[Byte] = {

    val classReader = new ClassReader(classBytesStream(decoderTemplateFqdn))
    val classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES)
    val classNode = new ClassNode()
    classReader.accept(classNode, ClassReader.EXPAND_FRAMES)

    val decodeMethod = findDecodeMethod(classNode.methods)
    val di = decodeMethod.instructions
    processMarkers(di, useMap)

    classNode.accept(classWriter)
    classWriter.toByteArray
  }
}
