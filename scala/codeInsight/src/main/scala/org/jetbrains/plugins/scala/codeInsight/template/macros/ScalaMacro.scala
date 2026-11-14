package org.jetbrains.plugins.scala.codeInsight.template.macros

import consulo.localize.LocalizeValue
import consulo.language.editor.template.`macro`.Macro
import consulo.language.editor.template.context.TemplateContextType
import org.jetbrains.plugins.scala.codeInsight.template.impl.ScalaCodeContextType

abstract class ScalaMacro extends Macro {

  def getNameShort: String

  override final def getName: String = "scala_" + getNameShort

  override def getPresentableName: LocalizeValue = LocalizeValue.of(getNameShort + "()")

  override final def isAcceptableInContext(context: TemplateContextType): Boolean =
    context match {
      case _: ScalaCodeContextType => true
      case _ => false
    }
}

object ScalaMacro {
  private[macros] val DefaultValue = "a"
}
