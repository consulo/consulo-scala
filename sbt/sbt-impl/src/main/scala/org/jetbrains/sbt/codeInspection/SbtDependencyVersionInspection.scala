package org.jetbrains.sbt.codeInspection

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.inspection.LocalQuickFix
import consulo.project.Project
import consulo.util.lang.StringUtil
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.codeInspection.AbstractFixOnPsiElement
import org.jetbrains.plugins.scala.extensions.&
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil.inNameContext
import org.jetbrains.plugins.scala.lang.psi.api.base.literals.ScStringLiteral
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScReferencePattern
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScReferenceExpression
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScPatternDefinition
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createExpressionFromText
import org.jetbrains.plugins.scala.packagesearch.codeInspection.DependencyVersionInspection
import org.jetbrains.plugins.scala.packagesearch.codeInspection.DependencyVersionInspection.DependencyDescriptor
import org.jetbrains.plugins.scala.project.ScalaFeatures
import org.jetbrains.sbt.SbtBundle

@ExtensionImpl
class SbtDependencyVersionInspection extends DependencyVersionInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(SbtBundle.message("packagesearch.dependency.version.inspection.description"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(SbtBundle.message("group.sbt"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WEAK_WARNING

  override def getShortName: String = "SbtDependencyVersionInspection"

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override protected def isAvailable(element: PsiElement): Boolean =
    SbtCodeInspectionUtils.isAvailable(element)

  override protected def createDependencyDescriptor(element: PsiElement): Option[DependencyDescriptor] =
    SbtCodeInspectionUtils.createDependencyDescriptor(element)

  override protected def createQuickFix(element: PsiElement, newerVersion: String): LocalQuickFix =
    new SbtUpdateDependencyVersionQuickFix(element, newerVersion)
}

class SbtUpdateDependencyVersionQuickFix(elem: PsiElement, newVer: String)
  extends AbstractFixOnPsiElement(SbtBundle.message("packagesearch.update.dependency.to.newer.version", newVer), elem) {

  override protected def doApplyFix(element: PsiElement)
                                   (implicit project: Project): Unit = {
    val features = ScalaFeatures.default
    element match {
      case str: ScStringLiteral =>
        str.replace(createExpressionFromText("\"" + StringUtil.escapeStringCharacters(newVer) + "\"", features))
      case ref: ScReferenceExpression =>
        ref.resolve() match {
          case (_: ScReferencePattern) & inNameContext(ScPatternDefinition.expr(expr)) => expr match {
            case str: ScStringLiteral =>
              str.replace(createExpressionFromText("\"" + StringUtil.escapeStringCharacters(newVer) + "\"", features))
            case _ =>
          }
        }
      case _ => // do nothing
    }
  }
}
