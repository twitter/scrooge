package com.twitter.scrooge.java_generator

import com.twitter.scrooge.ast._
import com.twitter.scrooge.frontend.ScroogeInternalException

// The rendered represents what gets printed to the output stream.  value is what render_const_value returns
class ConstValue(val rendered: String, val value: String)

class PrintConstController(
    val name: String,
    fieldType: FieldType,
    value: RHS,
    generator: ApacheJavaGenerator,
    ns: Option[Identifier],
    val in_static: Boolean = false,
    val defval: Boolean = false)
  extends BaseController(generator, ns) {
  val field_type = new FieldTypeController(fieldType, generator)
  def rendered_value = renderConstValue(value, fieldType).value

  def map_values = {
    val values = value.asInstanceOf[MapRHS]
    val mapType = fieldType.asInstanceOf[MapType]
    values.elems map { case (k, v) =>
      val renderedKey = renderConstValue(k, mapType.keyType)
      val renderedValue = renderConstValue(v, mapType.valueType)
      Map("key" -> renderedKey.value, "value" -> renderedValue.value, "rendered_key" -> renderedKey.rendered,
        "rendered_value" -> renderedValue.rendered)
    }
  }

  def list_or_set_values = {
    value match {
      case SetRHS(elems) => {
        val setType = fieldType.asInstanceOf[SetType]
        elems.map { v =>
          val renderedValue = renderConstValue(v, setType.eltType)
          Map("value" -> renderedValue.value, "rendered_value" -> renderedValue.rendered)
        }
      }
      case ListRHS(elems) => {
        val listType = fieldType.asInstanceOf[ListType]
        elems.map { v =>
          val renderedValue = renderConstValue(v, listType.eltType)
          Map("value" -> renderedValue.value, "rendered_value" -> renderedValue.rendered)
        }
      }
      case _ => throw new ScroogeInternalException("Invalid state PrintConstController")
    }
  }

  def struct_values = {
    val values = value.asInstanceOf[StructRHS].elems
    val structType = fieldType.asInstanceOf[StructType]
    for {
      f <- structType.struct.fields
      v <- values.get(f)
    } yield {
      val renderedValue = renderConstValue(v, f.fieldType)
      Map("key" -> f.sid.name, "value" -> renderedValue.value, "rendered_value" -> renderedValue.rendered)
    }
  }

  private def renderConstValue(constant: RHS, fieldType: FieldType): ConstValue = {
    fieldType match {
      case TString => {
        val constValue = constant.asInstanceOf[StringLiteral].value
        new ConstValue(null, "\"" + constValue + "\"")
      }
      case TBool => {
        constant match {
          case intValue: IntLiteral =>
            new ConstValue(null, if (intValue.value > 0) "true" else "false")
          case bool: BoolLiteral =>
            new ConstValue(null, if (bool.value) "true" else "false")
          case _ => throw new ScroogeInternalException("BoolType has invalid value: " + constant)
        }
      }
      case TByte => new ConstValue(null, "(byte)" + constant.asInstanceOf[IntLiteral].value)
      case TI16 => new ConstValue(null, "(short)" + constant.asInstanceOf[IntLiteral].value)
      case TI32 => new ConstValue(null, constant.asInstanceOf[IntLiteral].value.toString)
      case TI64 => new ConstValue(null, constant.asInstanceOf[IntLiteral].value + "L")
      case TDouble => {
        constant match {
          case DoubleLiteral(value) => {
            // TODO: this is here to match apache code but probably can be removed.
            if (value.floor == value) {
              new ConstValue(null, value.toInt.toString)
            } else {
              new ConstValue(null, value.toString)
            }
          }
          case IntLiteral(value) => new ConstValue(null, "(double)" + value.toString)
          case _ => throw new ScroogeInternalException("Invalid state renderConstValue")
        }
      }
      case EnumType(enumValue, scope) => {
        val ordinalValue = constant match {
          case intValue: IntLiteral => intValue.value.toInt
          case enumValue: EnumRHS => enumValue.value.value
          case _ => throw new ScroogeInternalException("Invalid state for renderConstValue")
        }
        val namedValue = enumValue.values filter { v =>
          v.value == ordinalValue
        }
        if (namedValue.isEmpty) {
          throw new ScroogeInternalException("Enum value not found")
        } else {
          val enumFqn = generator.qualifyNamedType(enumValue.sid, scope)
          val enumValueFqn = namedValue(0).sid.addScope(enumFqn)
          new ConstValue(null, enumValueFqn.fullName)
        }
      }
      case _ => {
        val tmpVal = generator.tmp()
        new ConstValue(generator.printConstValue(tmpVal, fieldType, constant, ns, in_static = true), tmpVal)
      }
    }
  }
}
