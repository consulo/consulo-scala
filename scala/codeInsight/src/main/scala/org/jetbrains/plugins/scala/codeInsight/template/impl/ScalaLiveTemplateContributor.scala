package org.jetbrains.plugins.scala.codeInsight.template.impl

import consulo.annotation.component.ExtensionImpl
import consulo.language.editor.template.LiveTemplateContributor
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.editor.ScalaEditorBundle

import scala.util.Using

// upstream: liveTemplates/scala.xml registered via <defaultLiveTemplates/>
@ExtensionImpl
final class ScalaLiveTemplateContributor extends LiveTemplateContributor {

  override def groupId(): String = "scala"

  override def groupName(): LocalizeValue = LocalizeValue.of("Scala")

  override def contribute(factory: LiveTemplateContributor.Factory): Unit = {
    def template(name: String, value: String, description: LocalizeValue)
                (body: LiveTemplateContributor.Builder => Unit): Unit =
      Using.resource(factory.newBuilder("scala" + name.capitalize, name, value, description))(body)

    def bundle(key: String): LocalizeValue = LocalizeValue.of(ScalaEditorBundle.message(key))

    template("main", "def main(args: Array[String]): Unit = {\n  $END$\n}", bundle("template.for.main.method")) { builder =>
      builder.withReformat()
      builder.withContext(classOf[ScalaBlankLineContextType], true)
    }

    template("opt", "$OPTION$ match {\n  case Some($NAME$) => $END$\n  case None =>\n}", bundle("template.for.matching.option.type")) { builder =>
      builder.withVariable("OPTION", "scalaVariableOfType(\"scala.Option\")", "option", true)
      builder.withVariable("NAME", "suggestScalaVariableName(\"option\", OPTION)", "x", true)
      builder.withContext(classOf[ScalaCodeContextType], true)
    }

    template("itr", "for ($NAME$ <- $FOR$ $GUARD$) {\n  $END$\n  }", bundle("template.for.scala.for")) { builder =>
      builder.withReformat()
      builder.withVariable("FOR", "scalaVariableOfType(\"foreach\")", "option", true)
      builder.withVariable("NAME", "suggestScalaVariableName(\"foreach\", FOR)", "x", true)
      builder.withVariable("GUARD", "", "", true)
      builder.withContext(classOf[ScalaCodeContextType], true)
    }

    template("itry", "for ($NAME$ <- $FOR$ $GUARD$) yield {\n  $END$\n  }", bundle("template.for.scala.for.yield")) { builder =>
      builder.withReformat()
      builder.withVariable("FOR", "scalaVariableOfType(\"foreach\")", "option", true)
      builder.withVariable("NAME", "suggestScalaVariableName(\"foreach\", FOR)", "x", true)
      builder.withVariable("GUARD", "", "", true)
      builder.withContext(classOf[ScalaCodeContextType], true)
    }

    template("priv", "println(s\"$EXPR_COPY$ = $${$EXPR$}\")", bundle("template.for.expressions.output")) { builder =>
      builder.withReformat()
      builder.withVariable("EXPR", "scalaVariableOfType(\"\")", "false", true)
      builder.withVariable("EXPR_COPY", "EXPR", "", false)
      builder.withContext(classOf[ScalaCodeContextType], true)
    }

    template("T", "<$TAG$>$SELECTION$</$TAGNAME$>\n", bundle("surround.with.xml")) { builder =>
      builder.withReformat()
      builder.withVariable("TAG", "", "", true)
      builder.withVariable("SELECTION", "", "", false)
      builder.withVariable("TAGNAME", "firstWord(TAG)", "\"\"", false)
      builder.withContext(classOf[ScalaXmlContextType], true)
    }

    template("unapply", "def unapply$TYPE_ARGS$(arg: $CLASS_NAME$$TYPE_ARGS_SHORT$): Option[$RET_TYPE$] = $BODY$$END$", bundle("template.for.unapply")) { builder =>
      builder.withReformat()
      builder.withVariable("TYPE_ARGS", "scala_typeParams(scala_companionClass())", "", false)
      builder.withVariable("TYPE_ARGS_SHORT", "scala_typeParamsWithoutBounds(scala_companionClass())", "", false)
      builder.withVariable("CLASS_NAME", "scala_className", "X", false)
      builder.withVariable("RET_TYPE", "scala_primaryConstructorParamTypes", "Int", true)
      builder.withVariable("BODY", "", "\"???\"", true)
      builder.withContext(classOf[ScalaBlankLineContextType], true)
    }

    template("apply", "def apply $TYPE_ARGS$($ARGS$): $CLASS_NAME$$TYPE_ARGS_SHORT$ = new $CLASS_NAME$($ARGS_NO_TYPES$)", bundle("template.for.apply")) { builder =>
      builder.withReformat()
      builder.withVariable("TYPE_ARGS", "scala_typeParams(scala_companionClass())", "", false)
      builder.withVariable("TYPE_ARGS_SHORT", "scala_typeParamsWithoutBounds(scala_companionClass())", "", false)
      builder.withVariable("ARGS", "scala_primaryConstructorParams", "x", true)
      builder.withVariable("CLASS_NAME", "scala_className", "", false)
      builder.withVariable("ARGS_NO_TYPES", "scala_primaryConstructorParamNames(ARGS)", "", true)
      builder.withContext(classOf[ScalaBlankLineContextType], true)
    }

    template("todo", "// TODO: $todo$ ", LocalizeValue.localizeTODO("adds // TODO")) { builder =>
      builder.withReformat()
      builder.withVariable("todo", "", "", true)
      builder.withContext(classOf[ScalaCodeContextType], true)
    }

    template("imc", "implicit class $CLASS_NAME$$SUFFIX$(private val $RECEIVER_NAME$: $TYPE_NAME$)$EXTENDS_ANY_VAL${\n$END$\n}", bundle("template.for.extension.methods.container")) { builder =>
      builder.withReformat()
      builder.withVariable("TYPE_NAME", "", "\"Any\"", true)
      builder.withVariable("RECEIVER_NAME", "scala_suggestVariableNameByTypeText(TYPE_NAME)", "\"target\"", true)
      builder.withVariable("CLASS_NAME", "scala_implicitValueClassName(TYPE_NAME)", "", false)
      builder.withVariable("EXTENDS_ANY_VAL", "scala_implicitValueExtendsValueClassOptionalText", "", false)
      builder.withContext(classOf[ScalaCodeContextType], false)
      builder.withContext(classOf[ScalaImplicitClassContextType], true)
    }
  }
}
