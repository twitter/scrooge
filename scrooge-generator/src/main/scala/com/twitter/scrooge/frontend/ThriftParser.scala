/*
 * Copyright 2011 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twitter.scrooge.frontend

import java.util.logging.Logger
import com.twitter.scrooge.ast._
import com.twitter.scrooge.backend.{ServiceOption, WithAsClosable}
import java.io.FileNotFoundException
import scala.collection.concurrent.{Map, TrieMap}
import scala.collection.mutable
import scala.util.parsing.combinator._
import scala.collection.immutable
import scala.util.matching.Regex

class ThriftParser(
  importer: Importer,
  strict: Boolean,
  defaultOptional: Boolean = false,
  skipIncludes: Boolean = false,
  documentCache: Map[String, Document] = new TrieMap[String, Document]
)(
  implicit val logger: Logger = Logger.getLogger(getClass.getName))
    extends RegexParsers {

  //                            1    2           3                     4         4a    4b    4c       4d
  override val whiteSpace: Regex =
    """(\s+|(//.*\r?\n)|(#([^@\r\n].*)?\r?\n)|(/\*[^\*]([^\*]+|\r?\n|\*(?!/))*\*/))+""".r
  // 1: whitespace, 1 or more
  // 2: leading // followed by anything 0 or more, until newline
  // 3: leading #  then NOT a @ followed by anything 0 or more, until newline
  // 4: leading /* then NOT a *, then...
  // 4a:  not a *, 1 or more times
  // 4b:  OR a newline
  // 4c:  OR a * followed by a 0-width lookahead / (not sure why we have this -KO)
  //   (0 or more of 4b/4c/4d)
  // 4d: ending */

  // transformations

  def fixFieldIds(fields: List[Field]): List[Field] = {
    // check negative field ids
    fields.find(_.index < 0).foreach { field => throw new NegativeFieldIdException(field.sid.name) }

    // check duplicate field ids
    fields
      .filter(_.index != 0)
      .foldLeft(Set[Int]())((set, field) => {
        if (set.contains(field.index)) {
          throw new DuplicateFieldIdException(field.sid.name)
        } else {
          set + field.index
        }
      })

    var nextId = -1
    fields.map { field =>
      if (field.index == 0) {
        val f = field.copy(index = nextId)
        nextId -= 1
        f
      } else {
        field
      }
    }
  }

  // identifier

  /**
   * The places where both SimpleIDs and QualifiedIDs are allowed, in which case
   * we use Identifier:
   *   - right hand side of an assignment
   *   - namespace declaration
   * For all other places, only SimpleIDs are allowed. Specifically
   *   - right hand side of an assignment.
   *
   * Note that Scala parser does not support left recursion well. We cannot do
   * something like this which is more intuitive:

      def qualifiedID = (simpleID <~ "\\.") ~ repsep(simpleID, "\\.".r) ^^ {
        case id ~ ids => QualifiedID((id +: ids) map { _.name })
      }

      def identifier: Parser[Identifier] = qualifiedID | simpleID
   */
  val identifierRegex: Regex = "[A-Za-z_][A-Za-z0-9\\._]*".r
  lazy val identifier: Parser[Identifier] = positioned(identifierRegex ^^ { x => Identifier(x) })

  private[this] val thriftKeywords = Set[String](
    "async",
    "const",
    "enum",
    "exception",
    "extends",
    "include",
    "namespace",
    "optional",
    "required",
    "service",
    "struct",
    "throws",
    "typedef",
    "union",
    "void",
    // Built-in types are also keywords.
    "binary",
    "bool",
    "byte",
    "double",
    "i16",
    "i32",
    "i64",
    "list",
    "map",
    "set",
    "string"
  )

  lazy val simpleIDRegex: Regex = "[A-Za-z_][A-Za-z0-9_]*".r
  lazy val simpleID: Parser[SimpleID] = positioned(simpleIDRegex ^^ { x =>
    if (thriftKeywords.contains(x))
      failOrWarn(new KeywordException(x))

    SimpleID(x)
  })

  // right hand side (RHS)

  lazy val rhs: Parser[RHS] = positioned({
    numberLiteral | boolLiteral | stringLiteral | listOrMapRHS | mapRHS | idRHS |
      failure("constant expected")
  })

  lazy val boolLiteral: Parser[BoolLiteral] = ("true" | "True" | "false" | "False") ^^ { x =>
    if (x.toLowerCase == "false") BoolLiteral(false)
    else BoolLiteral(true)
  }

  lazy val intConstant: Parser[IntLiteral] = "[-+]?\\d+(?!\\.)".r ^^ { x => IntLiteral(x.toLong) }

  lazy val numberLiteral: Parser[Literal] = "[-+]?\\d+(\\.\\d+)?([eE][-+]?\\d+)?".r ^^ { x =>
    if (x.exists { c => "eE." contains c }) DoubleLiteral(x.toDouble)
    else IntLiteral(x.toLong)
  }

  // use a single regex to match string quote-to-quote, so that whitespace parser doesn't
  // get executed inside the quotes
  lazy val doubleQuotedString: Regex = """(")(\\.|[^\\"])*(")""".r
  lazy val singleQuotedString: Regex = """'(\\.|[^\\'])*'""".r

  lazy val stringLiteral: Parser[StringLiteral] = (doubleQuotedString | singleQuotedString) ^^ {
    // strip off quotes
    x => StringLiteral(x.substring(1, x.length - 1))
  }

  lazy val listSeparator: Regex = "[,;]?".r

  lazy val listOrMapRHS: Parser[ListRHS] =
    "[" ~> repsep(rhs, listSeparator) <~ opt(listSeparator) <~ "]" ^^ { list => ListRHS(list) }

  lazy val keyval: Parser[(RHS, RHS)] = rhs ~ (":" ~> rhs) ^^ {
    case k ~ v => (k, v)
  }

  lazy val mapRHS: Parser[MapRHS] =
    "{" ~> repsep(keyval, listSeparator) <~ opt(listSeparator) <~ "}" ^^ { list => MapRHS(list) }

  lazy val idRHS: Parser[IdRHS] = identifier ^^ { id => IdRHS(id) }

  // types

  lazy val fieldType: Parser[FieldType] = baseType | containerType | referenceType

  lazy val referenceType: Parser[ReferenceType] = identifier ^^ { id => ReferenceType(id) }

  lazy val baseType: Parser[BaseType] = (
    "bool" ^^^ TBool |
      "byte" ^^^ TByte |
      "i8" ^^^ TByte |
      "i16" ^^^ TI16 |
      "i32" ^^^ TI32 |
      "i64" ^^^ TI64 |
      "double" ^^^ TDouble |
      "string" ^^^ TString |
      "binary" ^^^ TBinary
  )

  lazy val containerType: Parser[ContainerType] = mapType | setType | listType

  lazy val mapType: Parser[MapType] =
    ("map" ~> opt(cppType) <~ "<") ~ (fieldType <~ ",") ~ (fieldType <~ ">") ^^ {
      case cpp ~ key ~ value => MapType(key, value, cpp)
    }

  lazy val setType: Parser[SetType] = ("set" ~> opt(cppType)) ~ ("<" ~> fieldType <~ ">") ^^ {
    case cpp ~ t => SetType(t, cpp)
  }

  lazy val listType: Parser[ListType] = ("list" ~ "<") ~> (fieldType <~ ">") ~ opt(cppType) ^^ {
    case t ~ cpp => ListType(t, cpp)
  }

  // FFS. i'm very close to removing this and forcably breaking old thrift files.
  lazy val cppType: Parser[String] = "cpp_type" ~> stringLiteral ^^ { literal => literal.value }

  // Cast IntLiterals into booleans.
  private[this] def convertRhs(fieldType: FieldType, rhs: RHS): RHS = {
    fieldType match {
      case TBool =>
        rhs match {
          case x: BoolLiteral => x
          case IntLiteral(0) => BoolLiteral(false)
          case IntLiteral(1) => BoolLiteral(true)
          case _ => throw new TypeMismatchException(s"Can't assign $rhs to a bool", rhs)
        }
      case _ => rhs
    }
  }

  // fields

  lazy val field: Parser[Field] = positioned(
    (opt(comments) ~ opt(fieldId) ~ fieldReq) ~
      (fieldType ~ defaultedAnnotations ~ simpleID) ~
      opt("=" ~> rhs) ~ defaultedAnnotations <~ opt(listSeparator) ^^ {
      case (comm ~ fid ~ req) ~ (ftype ~ typeAnnotations ~ sid) ~ value ~ fieldAnnotations =>
        val transformedVal = value.map(convertRhs(ftype, _))

        val transformedReq =
          if (!defaultOptional && transformedVal.isDefined && req.isOptional) Requiredness.Default
          else req

        Field(
          fid.getOrElse(0),
          sid,
          sid.name,
          ftype,
          transformedVal,
          transformedReq,
          typeAnnotations,
          fieldAnnotations,
          comm
        )
    }
  )

  lazy val fieldId: Parser[Int] = intConstant <~ ":" ^^ { x => x.value.toInt }

  lazy val fieldReq: Parser[Requiredness] = opt("required" | "optional") ^^ {
    case Some("required") => Requiredness.Required
    case Some("optional") => Requiredness.Optional
    case Some(r) => throw new ParseException(s"Invalid requiredness value '$r'")
    case None => Requiredness.Default
  }

  // functions

  lazy val function: Parser[Function] =
    (opt(comments) ~ (opt("oneway") ~ functionType)) ~ (simpleID <~ "(") ~ (rep(
      field
    ) <~ ")") ~
      opt(throws) ~ defaultedAnnotations <~ opt(listSeparator) ^^ {
      case comment ~ (oneway ~ ftype) ~ id ~ args ~ throws ~ annotations =>
        Function(
          id,
          id.name,
          if (oneway.isDefined) OnewayVoid else ftype,
          fixFieldIds(args),
          throws.map(fixFieldIds).getOrElse(Nil),
          comment,
          annotations
        )
    }

  lazy val functionType: Parser[FunctionType] = ("void" ^^^ Void) | fieldType

  lazy val throws: Parser[List[Field]] = "throws" ~> "(" ~> rep(field) <~ ")"

  // definitions

  lazy val definition: Parser[Definition] =
    const | typedef | enum | senum | struct | union | exception | service

  lazy val const: Parser[ConstDefinition] =
    opt(comments) ~ ("const" ~> fieldType) ~ simpleID ~ ("=" ~> rhs) ~ opt(
      listSeparator
    ) ^^ {
      case comment ~ ftype ~ sid ~ const ~ _ =>
        ConstDefinition(sid, ftype, convertRhs(ftype, const), comment)
    }

  lazy val typedef: Parser[Typedef] =
    (opt(comments) ~ "typedef") ~> fieldType ~ defaultedAnnotations ~ simpleID ~ defaultedAnnotations ^^ {
      case dtype ~ referentAnnotations ~ sid ~ aliasAnnotations =>
        Typedef(sid, dtype, referentAnnotations, aliasAnnotations)
    }

  lazy val enum: Parser[Enum] = (opt(comments) ~ (("enum" ~> simpleID) <~ "{")) ~ rep(
    opt(comments) ~ simpleID ~ opt("=" ~> intConstant) ~ defaultedAnnotations <~
      opt(listSeparator)
  ) ~ ("}" ~> defaultedAnnotations) ^^ {
    case comment ~ sid ~ items ~ annotation =>
      var failed: Option[Int] = None
      val seen = new mutable.HashSet[Int]
      var nextValue = 0
      val values = new mutable.ListBuffer[EnumField]
      items.foreach {
        case c ~ k ~ v ~ a =>
          val value = v
            .map {
              _.value.toInt
            }
            .getOrElse(nextValue)
          if (seen contains value) failed = Some(value)
          nextValue = value + 1
          seen += value
          values += EnumField(k, value, c, a)
      }
      if (failed.isDefined) {
        throw new RepeatingEnumValueException(sid.name, failed.get)
      } else {
        Enum(sid, values.toList, comment, annotation)
      }
  }

  lazy val senum: Parser[Senum] =
    (("senum" ~> simpleID) <~ "{") ~ (rep(stringLiteral <~ opt(listSeparator)) <~
      "}") ~ defaultedAnnotations ^^ {
      case sid ~ items ~ annotations =>
        Senum(sid, items.map {
          _.value
        }, annotations)
    }

  def structLike(
    keyword: String
  ): Parser[Option[String] ~ SimpleID ~ List[Field] ~ immutable.Map[String, String]] =
    (opt(comments) ~ ((keyword ~> simpleID) <~ "{")) ~ rep(field) ~ ("}" ~> defaultedAnnotations)

  lazy val struct: Parser[Struct] = positioned(structLike("struct") ^^ {
    case comment ~ sid ~ fields ~ annotations =>
      Struct(sid, sid.name, fixFieldIds(fields), comment, annotations)
  })

  private[this] val disallowedUnionFieldNames =
    Set("unknown_union_field", "unknownunionfield") map {
      _.toLowerCase
    }

  lazy val union: Parser[Union] = positioned(structLike("union") ^^ {
    case comment ~ sid ~ fields ~ annotations =>
      val fields0 = fields.map {
        case f if f.requiredness == Requiredness.Default =>
          if (disallowedUnionFieldNames.contains(f.sid.name.toLowerCase)) {
            throw new UnionFieldInvalidNameException(sid.name, f.sid.name)
          } else f
        case f =>
          failOrWarn(UnionFieldRequirednessException(sid.name, f.sid.name, f.requiredness.toString))
          f.copy(requiredness = Requiredness.Default)
      }

      Union(sid, sid.name, fixFieldIds(fields0), comment, annotations)
  })

  lazy val exception: Parser[Exception_] =
    (opt(comments) ~ ("exception" ~> simpleID <~ "{")) ~ opt(rep(field)) ~
      ("}" ~> defaultedAnnotations) ^^ {
      case comment ~ sid ~ fields ~ annotations =>
        Exception_(sid, sid.name, fixFieldIds(fields.getOrElse(Nil)), comment, annotations)
    }

  lazy val service: Parser[Service] =
    (opt(comments) ~ ("service" ~> simpleID)) ~ opt("extends" ~> serviceParentID) ~ ("{" ~> rep(
      function
    ) <~
      "}") ~ defaultedAnnotations ^^ {
      case comment ~ sid ~ extend ~ functions ~ annotations => {
        val hasFuncNamedAsClosable =
          functions.exists(_.funcName.name.equalsIgnoreCase(WithAsClosable.AsClosableMethodName))
        val options: Set[ServiceOption] = if (hasFuncNamedAsClosable) {
          logger.warning(
            s"Generating user defined asClosable instead of default one for ${sid.fullName}"
          )
          Set.empty
        } else {
          Set(WithAsClosable)
        }
        Service(sid, extend, functions, comment, annotations, options)
      }
    }

  // This is a simpleID without the keyword check. Filenames that are thrift keywords are allowed.
  lazy val serviceParentID: Parser[ServiceParent] = opt(simpleIDRegex <~ ".") ~ simpleID ^^ {
    case prefix ~ sid =>
      ServiceParent(sid, prefix.map(SimpleID(_)))
  }

  // document

  lazy val document: Parser[Document] = rep(header) ~ rep(definition) <~ opt(comments) ^^ {
    case hs ~ ds => Document(hs, ds)
  }

  lazy val header: Parser[Header] = include | cppInclude | namespace

  lazy val include: Parser[Include] = opt(comments) ~> "include" ~> positioned(stringLiteral ^^ {
    s =>
      val doc =
        if (skipIncludes) {
          Document(Seq(), Seq())
        } else {
          parseFile(s.value)
        }
      Include(s.value, doc)
  })

  // bogus dude.
  lazy val cppInclude: Parser[CppInclude] = "cpp_include" ~> stringLiteral ^^ { s =>
    CppInclude(s.value)
  }

  lazy val namespace: Parser[Namespace] =
    opt(comments) ~> opt("#@") ~> "namespace" ~> namespaceScope ~ identifier ^^ {
      case scope ~ id =>
        Namespace(scope, id)
    }

  lazy val namespaceScope: Parser[String] = "*" ^^^ "*" | (identifier ^^ { _.fullName })

  /**
   * Matches scaladoc/javadoc style comments.
   */
  lazy val comments: Parser[String] = {
    rep1(docComment) ^^ {
      case cs =>
        cs.filterNot(_.replaceFirst("/*", "").contains("/*")) // omit invalid comment
          .mkString("\n")
    }
  }

  val docComment: Parser[String] = """(?s)/\*\*.+?\*/""".r

  // annotations

  lazy val annotation: Parser[(String, String)] = identifier ~ ("=" ~> stringLiteral) ^^ {
    case id ~ StringLiteral(value) => id.fullName -> value
  }

  lazy val annotationGroup: Parser[immutable.Map[String, String]] =
    "(" ~> repsep(annotation, ",") <~ (opt(",") ~ ")") ^^ { _.toMap }

  lazy val defaultedAnnotations: Parser[immutable.Map[String, String]] = opt(annotationGroup) ^^ {
    _ getOrElse Map.empty
  }

  def parse[T](in: String, parser: Parser[T], file: Option[String] = None): T =
    parseAll(parser, in) match {
      case Success(result, _) => result
      case x @ Failure(msg, z) => throw new ParseException(x.toString())
      case x @ Error(msg, _) => throw new ParseException(x.toString())
    }

  def parseFile(filename: String): Document = {
    importer.getResolvedPath(filename) match {
      // Cache the result if the importer supports caching.
      case Some(key) => documentCache.getOrElseUpdate(key, parseFileUncached(filename))
      // Else, just resolve the document.
      case None => parseFileUncached(filename)
    }
  }

  private[this] def parseFileUncached(filename: String): Document = {
    val contents = importer(filename) getOrElse {
      throw new FileNotFoundException(filename)
    }
    // one thrift file can be included in another and referenced like this:
    // list<includedthriftfilenamehere.Request> requests
    //
    // thus we need to ensure includedthriftfilenamehere is valid, otherwise the first person
    // to include the thrift file, with for example a dash in the name, will run into problems
    contents.thriftFilename foreach { f =>
      identifierRegex.findFirstIn(f) match {
        case Some(`f`) => ()
        case _ => failOrWarn(new InvalidThriftFilenameException(f, identifierRegex.toString()))
      }
    }

    val newParser = new ThriftParser(
      contents.importer,
      this.strict,
      this.defaultOptional,
      this.skipIncludes,
      this.documentCache
    )
    try {
      newParser.parse(contents.data, newParser.document, contents.thriftFilename)
    } catch {
      case e: Throwable => throw new FileParseException(filename, e)
    }

  }

  // helper functions
  def failOrWarn(ex: ParseWarning): Unit = {
    if (strict)
      throw ex
    else
      logger.warning(ex.getMessage)
  }
}
