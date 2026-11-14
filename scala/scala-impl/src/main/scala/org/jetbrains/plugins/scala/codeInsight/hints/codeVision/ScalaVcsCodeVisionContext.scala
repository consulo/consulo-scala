//noinspection ApiStatus,UnstableApiUsage
package org.jetbrains.plugins.scala.codeInsight.hints.codeVision

import consulo.annotation.component.ExtensionImpl
import consulo.versionControlSystem.codeVision.VcsCodeVisionLanguageContext
import consulo.language.Language
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.extensions.{ObjectExt, PsiElementExt}
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScExtension, ScFunction}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef._

@ExtensionImpl
final class ScalaVcsCodeVisionContext extends VcsCodeVisionLanguageContext {

  import ScalaVcsCodeVisionContext._

  override def isAccepted(element: PsiElement): Boolean =
    (isAcceptedTemplateDefinition(element) || isAcceptedMember(element)) &&
      !isInWorksheetFile(element) //SCL-21098

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  // upstream: handleClick logged JavaCodeVisionUsageCollector statistics (no Consulo counterpart)
}

private object ScalaVcsCodeVisionContext {
  private def isInWorksheetFile(element: PsiElement): Boolean =
    element.containingScalaFile.exists(_.isWorksheetFile)

  private def isAcceptedTemplateDefinition(element: PsiElement): Boolean =
    element.is[ScClass, ScObject, ScTrait, ScEnum, ScGivenDefinition]

  private def isAcceptedMember(element: PsiElement): Boolean = element match {
    case fn: ScFunction => isAcceptedMember(fn)
    case ext: ScExtension => isAcceptedMember(ext)
    case _ => false
  }

  private def isAcceptedMember(member: ScMember): Boolean =
    member.isTopLevel || isAcceptedTemplateDefinition(member.containingClass)
}
