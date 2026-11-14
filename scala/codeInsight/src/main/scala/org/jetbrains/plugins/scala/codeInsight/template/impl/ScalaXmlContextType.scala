package org.jetbrains.plugins.scala.codeInsight.template.impl

import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.template.context.TemplateActionContext
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle
import org.jetbrains.plugins.scala.lang.psi.api._
import org.jetbrains.plugins.scala.api.lang.psi.api._

@ExtensionImpl
final class ScalaXmlContextType
  extends ScalaFileTemplateContextType.ElementContextType("SCALA_XML", ScalaCodeInsightBundle.message("element.context.type.xml")) {

  protected def isInContextInScalaFile(context: TemplateActionContext)(implicit file: ScalaFile): Boolean =
    ScalaFileTemplateContextType.isInContext(context, classOf[expr.xml.ScXmlExpr])()
}
