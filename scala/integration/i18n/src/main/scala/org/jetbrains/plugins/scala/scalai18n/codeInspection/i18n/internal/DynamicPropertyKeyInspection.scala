package org.jetbrains.plugins.scala.scalai18n.codeInspection.i18n.internal

import com.intellij.java.language.codeInsight.AnnotationUtil
import consulo.annotation.component.ExtensionImpl
import consulo.language.editor.inspection.ProblemsHolder
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.language.psi.PsiElementVisitor
import org.jetbrains.plugins.scala.codeInspection.PsiElementVisitorSimple
import org.jetbrains.plugins.scala.lang.psi.api.base.literals.ScStringLiteral
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.scalai18n.codeInspection.i18n.ScalaI18nUtil

@ExtensionImpl
class DynamicPropertyKeyInspection
  extends ScalaI18nInternalInspection("internal.only.pass.hardcoded.strings.as.property.keys", HighlightDisplayLevel.ERROR) {

  override protected def buildInternalVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case s: ScStringLiteral if s.isSimpleLiteral =>
    case e: ScExpression =>
      val isPassedToProperty = ScalaI18nUtil.isPassedToAnnotated(e, AnnotationUtil.PROPERTY_KEY)
      if (isPassedToProperty)
        holder.registerProblem(e, getDisplayName.get)
    case _ =>
  }
}
