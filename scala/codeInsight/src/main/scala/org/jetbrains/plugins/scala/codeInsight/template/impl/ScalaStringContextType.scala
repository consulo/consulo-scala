package org.jetbrains.plugins.scala
package codeInsight
package template
package impl

import consulo.annotation.component.ExtensionImpl

import com.intellij.codeInsight.template.TemplateActionContext
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle
import org.jetbrains.plugins.scala.lang.psi.api._
import org.jetbrains.plugins.scala.api.lang.psi.api._
import org.jetbrains.plugins.scala.lang.psi.api.base.literals.ScStringLiteral

@ExtensionImpl
final class ScalaStringContextType
  extends ScalaFileTemplateContextType.ElementContextType(ScalaCodeInsightBundle.message("element.context.type.string")) {

  protected def isInContextInScalaFile(context: TemplateActionContext)(implicit file: ScalaFile): Boolean =
    ScalaStringContextType.isInContext(context)
}

object ScalaStringContextType {

  private[impl] def isInContext(context: TemplateActionContext)
                               (implicit file: ScalaFile): Boolean =
    ScalaFileTemplateContextType.isInContext(context, classOf[ScStringLiteral])(_.hasValidClosingQuotes)
}
