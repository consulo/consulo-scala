package org.jetbrains.plugins.scala.codeInsight.template.impl

import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.template.context.TemplateActionContext
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

@ExtensionImpl
final class ScalaCodeContextType
  extends ScalaFileTemplateContextType.ElementContextType("SCALA_CODE", ScalaCodeInsightBundle.message("element.context.type.code")) {

  protected def isInContextInScalaFile(context: TemplateActionContext)(implicit file: ScalaFile): Boolean = {
    !(ScalaCommentContextType.isInContext(context) ||
      ScalaStringContextType.isInContext(context))
  }
}
