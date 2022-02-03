package com.twitter.scrooge.swift_generator

import com.twitter.scrooge.ast.EnumType
import com.twitter.scrooge.ast.FieldType
import com.twitter.scrooge.ast.Identifier
import com.twitter.scrooge.ast.IntLiteral
import com.twitter.scrooge.ast.ListRHS
import com.twitter.scrooge.ast.ListType
import com.twitter.scrooge.ast.MapRHS
import com.twitter.scrooge.ast.MapType
import com.twitter.scrooge.ast.RHS
import com.twitter.scrooge.ast.SetRHS
import com.twitter.scrooge.ast.SetType
import com.twitter.scrooge.ast.StructRHS
import com.twitter.scrooge.ast.StructType
import com.twitter.scrooge.ast.TBool
import com.twitter.scrooge.ast.TByte
import com.twitter.scrooge.ast.TDouble
import com.twitter.scrooge.ast.TI16
import com.twitter.scrooge.ast.TI32
import com.twitter.scrooge.ast.TI64
import com.twitter.scrooge.ast.TString
import com.twitter.scrooge.frontend.ScroogeInternalException
import com.twitter.scrooge.java_generator.ConstValue
import com.twitter.scrooge.java_generator.PrintConstController

class MapConstantValue(val key: String, val value: String, val last: Boolean)
class CollectionConstantValue(val value: String, val last: Boolean)
class StructConstantValue(val name: String, val rendered_value: String, val last: Boolean)

class SwiftPrintConstController(
  name: String,
  fieldType: FieldType,
  value: RHS,
  generator: SwiftGenerator,
  ns: Option[Identifier],
  total: Int,
  in_static: Boolean = false,
  defval: Boolean = false,
  val public_interface: Boolean)
    extends PrintConstController(name, fieldType, value, generator, ns, in_static, defval) {

  def map_constant_values: Seq[MapConstantValue] = {
    val values = value.asInstanceOf[MapRHS]
    val mapType = fieldType.asInstanceOf[MapType]
    values.elems.zipWithIndex map {
      case ((key, value), i) =>
        val renderedKey = renderConstValue(key, mapType.keyType)
        val renderedValue = renderConstValue(value, mapType.valueType)
        new MapConstantValue(
          renderedKey.value,
          renderedValue.value,
          values.elems.size - 1 == i
        )
    }
  }

  def collection_constant_values: Iterable[CollectionConstantValue] = {
    value match {
      case SetRHS(elems) => {
        val setType = fieldType.asInstanceOf[SetType]
        elems.zipWithIndex map {
          case (v, i) =>
            val renderedValue = renderConstValue(v, setType.eltType)
            new CollectionConstantValue(renderedValue.value, elems.size - 1 == i)
        }
      }
      case ListRHS(elems) => {
        val listType = fieldType.asInstanceOf[ListType]
        elems.zipWithIndex map {
          case (v, i) =>
            val renderedValue = renderConstValue(v, listType.eltType)
            new CollectionConstantValue(renderedValue.value, elems.size - 1 == i)
        }
      }
      case _ => throw new ScroogeInternalException(s"Invalid state PrintConstController '$value'")
    }
  }

  def struct_constant_values: Seq[StructConstantValue] = {
    value match {
      case struct: StructRHS => {
        val values = value.asInstanceOf[StructRHS].elems
        val structType = fieldType.asInstanceOf[StructType]
        val feilds = for {
          f <- structType.struct.fields
          v <- values.get(f)
        } yield {
          (f, v)
        }

        feilds.zipWithIndex map {
          case ((f, v), i) =>
            val renderedValue = renderConstValue(v, f.fieldType)
            new StructConstantValue(f.sid.name, renderedValue.rendered, feilds.size - 1 == i)
        }
      }
      case _ => throw new ScroogeInternalException(s"Invalid state PrintConstController '$value'")
    }
  }

  override def renderConstValue(constant: RHS, fieldType: FieldType): ConstValue = {
    fieldType match {
      case TByte | TI16 | TI32 | TI64 =>
        new ConstValue(
          null,
          constant
            .asInstanceOf[IntLiteral]
            .value
            .toString)
      case TDouble => {
        constant match {
          case IntLiteral(value) => new ConstValue(null, value.toString)
          case _ => super.renderConstValue(constant, fieldType)
        }
      }
      case EnumType(_, _) | TString | TBool => super.renderConstValue(constant, fieldType)
      case _ => {
        val tmpVal = generator.tmp()
        new ConstValue(
          generator.printConstValue(tmpVal, fieldType, constant, ns, total = 0, defval = false),
          tmpVal
        )
      }
    }
  }
}
