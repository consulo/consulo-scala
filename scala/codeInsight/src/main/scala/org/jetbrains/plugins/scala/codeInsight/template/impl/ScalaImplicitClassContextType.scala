package org.jetbrains.plugins.scala.codeInsight.template.impl

import consulo.annotation.component.ExtensionImpl

import com.intellij.codeInsight.template.TemplateActionContext
import consulo.language.psi.PsiElement
import consulo.language.impl.psi.LeafPsiElement
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle
import org.jetbrains.plugins.scala.extensions.{&, ObjectExt, Parent}
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.base.ScReference

@ExtensionImpl
final class ScalaImplicitClassContextType
  extends ScalaFileTemplateContextType.ElementContextType(ScalaCodeInsightBundle.message("element.context.type.implicit.class")) {

  protected def isInContextInScalaFile(context: TemplateActionContext)(implicit file: ScalaFile): Boolean = {
    val offset = context.getStartOffset
    val element = file.findElementAt(offset) match {
      case (_: LeafPsiElement) & Parent(ref: ScReference) => ref // expected to be an injected synthetic reference (for completion)
      case el                                             => el
    }

    val isTopLevel = element == null || isTopLevelElement(element)
    if (isTopLevel)
      file.isWorksheetFile //`implicit` modifier cannot be used for top-level objects
    else
      true
  }

  private def isTopLevelElement(element: PsiElement): Boolean =
    element.getParent.is[ScalaFile]
}
