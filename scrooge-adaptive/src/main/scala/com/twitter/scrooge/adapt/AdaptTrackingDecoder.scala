package com.twitter.scrooge.adapt

import com.twitter.logging.Logger
import com.twitter.scrooge.{ThriftStruct, ThriftStructCodec}
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.atomic.AtomicInteger

private[adapt] object AdaptTrackingDecoder {
  val logger = Logger(this.getClass)

  val AdaptSuffix = "__Adapt"
  val AdaptDecoderSuffix = "__AdaptDecoder"
  val DecodeMethodName = "decode"
}

/**
 * A thrift decoder that adapts itself based on usage pattern of generated
 * thrift objects. Goal is to minimize costs for unused fields. This is done by
 * skipping unused fields during parsing and setting up a mechanism for them to
 * be decoded later on access. Delayed decoding is typically the regular eager
 * decoding so it's expensive because we end up doing decoding twice.
 * Expectation is that fields that are considered unused will rarely be accessed.
 * When fields are considered unused is decided using useThreshold.
 * @param fallbackDecoder Sometimes it may not be worth doing adaptive decoding,
 *                        fallback to this decoder in those cases.
 * @param accessRecordingDecoderBuilder Builder for decoder used during learning
 *                                      phase. Allows injecting AccessRecorder
 *                                      to learn about how fields are accessed.
 * @param settings        Settings that govern how adaptation is done
 * @param classLoader     ClassLoader used to load the adapted classes generated
 *                        at runtime.
 */
private[adapt] class AdaptTrackingDecoder[T <: ThriftStruct](
  codec: ThriftStructCodec[T],
  fallbackDecoder: Decoder[T],
  accessRecordingDecoderBuilder: AccessRecorder => Decoder[T],
  settings: AdaptSettings,
  classLoader: AdaptClassLoader)
    extends AccessRecorder
    with Decoder[T] {
  import AdaptTrackingDecoder._

  private[this] val trackedCount = new AtomicInteger()
  private[this] val fieldAccessCounts: Map[Short, AtomicInteger] =
    codec.metaData.fields.map { f =>
      (f.id, new AtomicInteger(0))
    }.toMap

  def fieldAccessed(fieldId: Short): Unit =
    fieldAccessCounts(fieldId).getAndIncrement()

  @volatile private[this] var adaptiveDecoder: Decoder[T] = _

  private[this] def allFieldsUsed(useMap: Map[Short, Boolean]): Boolean =
    useMap.values.forall(identity)

  private[this] def buildDecoder(): Decoder[T] = {
    val useMapByField = codec.metaData.fields.map { f =>
      (f, fieldAccessCounts(f.id).get >= settings.useThreshold)
    }.toMap

    val useMapByName = useMapByField.map {
      case (f, v) =>
        val normalizedName = CaseConverter.toCamelCase(f.name)
        (normalizedName, v)
    }

    val useMapById = useMapByField.map { case (f, v) => (f.id, v) }

    if (allFieldsUsed(useMapById)) {
      logger.ifDebug(
        s"Adaptive scrooge is using all fields for ${codec.metaData.structName} struct."
      )
      fallbackDecoder
    } else {
      val namesToUse = useMapByName.collect {
        case (name, true) => name
      }

      if (namesToUse.isEmpty) {
        logger.ifDebug(
          s"Adaptive scrooge isn't using fields for ${codec.metaData.structName} struct with settings: $settings"
        )
      } else {
        logger.ifDebug(
          s"Adaptive scrooge is using fields: ${namesToUse
            .mkString(",")} for ${codec.metaData.structName} struct with settings: $settings"
        )
      }
      buildAdaptiveDecoder(useMapByName, useMapById)
    }
  }

  private[this] def buildAdaptiveDecoder(
    useMapByName: Map[String, Boolean],
    useMapById: Map[Short, Boolean]
  ): Decoder[T] = {
    val codecClassName = codec.getClass.getName
    // Remove the trailing `$` of codecClassName,
    // codec can only be an object and the name of it should always end with `$`.
    val adaptFqdn = codecClassName.dropRight(1) + AdaptSuffix
    val adaptDecoderFqdn = codecClassName.dropRight(1) + AdaptDecoderSuffix

    // Prune AdaptTemplate to create Adapt and load it
    val adaptClassBytes = AdaptAsmPruner.pruneAdapt(adaptFqdn, useMapByName)
    classLoader.defineClass(adaptFqdn, adaptClassBytes)

    // Prune AdaptDecoderTemplate to create AdaptDecoder and load it
    val adaptDecoderClassBytes =
      AdaptAsmPruner.pruneAdaptDecoder(adaptDecoderFqdn, useMapById)

    val decoderClass = classLoader.defineClass(adaptDecoderFqdn, adaptDecoderClassBytes)
    val prunedDecoder = decoderClass.newInstance()

    val decodeMethod = decoderClass.getMethod(DecodeMethodName, classOf[AdaptTProtocol])

    new Decoder[T] {
      def apply(prot: AdaptTProtocol): T = {
        try {
          decodeMethod.invoke(prunedDecoder, prot).asInstanceOf[T]
        } catch {
          case e: InvocationTargetException if e.getCause != null =>
            // Throw the original exception if present
            throw e.getCause
        }
      }
    }
  }

  def apply(prot: AdaptTProtocol): T = {
    if (adaptiveDecoder != null) {
      adaptiveDecoder(prot)
    } else {

      /*
       * Note that we only block one event, one that makes trackedCount
       * reach settings.trackedReads, to build the decoder. Subsequent
       * events will continue to use accessRecordingDecoderBuilder until
       * adaptiveDecoder is built. At which point adaptiveDecoder takes
       * over.
       */
      if (trackedCount.incrementAndGet == settings.trackedReads + 1) {
        val decoder = buildDecoder()
        adaptiveDecoder = decoder
        decoder(prot)
      } else
        accessRecordingDecoderBuilder(this)(prot)
    }
  }
}
