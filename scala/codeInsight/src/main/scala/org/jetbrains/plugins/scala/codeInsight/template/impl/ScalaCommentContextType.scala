package org.jetbrains.plugins.scala.codeInsight.template.impl

import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.template.context.TemplateActionContext
import consulo.language.psi.{PsiComment, PsiWhiteSpace}
import consulo.language.psi.util.PsiTreeUtil
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

@ExtensionImpl
final class ScalaCommentContextType
  extends ScalaFileTemplateContextType.ElementContextType("SCALA_COMMENT", ScalaCodeInsightBundle.message("element.context.type.comment")) {

  protected def isInContextInScalaFile(context: TemplateActionContext)(implicit file: ScalaFile): Boolean =
    ScalaCommentContextType.isInContext(context)
}

object ScalaCommentContextType {

  private[impl] def isInContext(context: TemplateActionContext)
                               (implicit file: ScalaFile): Boolean = {
    val offset = context.getStartOffset
    val elementAtOffset = file.findElementAt(offset) match {
      case _: PsiWhiteSpace if offset > 0 => file.findElementAt(offset - 1)
      case element => element
    }

    PsiTreeUtil.getNonStrictParentOfType(elementAtOffset, classOf[PsiComment]) != null
  }
}
