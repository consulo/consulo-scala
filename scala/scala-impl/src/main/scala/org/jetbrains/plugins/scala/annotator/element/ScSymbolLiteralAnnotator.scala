package org.jetbrains.plugins.scala.annotator.element

import consulo.language.editor.inspection.ProblemHighlightType
import consulo.application.dumb.DumbAware
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.annotator.ScalaAnnotationHolder
import org.jetbrains.plugins.scala.annotator.quickfix.ConvertToExplicitSymbolQuickFix
import org.jetbrains.plugins.scala.lang.psi.api.base.literals.ScSymbolLiteral
import org.jetbrains.plugins.scala.project.ProjectPsiElementExt
import org.jetbrains.plugins.scala.api.project.ScalaLanguageLevel

object ScSymbolLiteralAnnotator extends ElementAnnotator[ScSymbolLiteral] with DumbAware {

  override def annotate(element: ScSymbolLiteral, typeAware: Boolean)
                       (implicit holder: ScalaAnnotationHolder): Unit = {

    val langLevel = element.scalaLanguageLevelOrDefault
    if (langLevel.isScala2 && langLevel >= ScalaLanguageLevel.Scala_2_13) {
      holder.createWarningAnnotation(
        element,
        ScalaBundle.message("symbolliterals.are.deprecated", element.contentText),
        ProblemHighlightType.LIKE_DEPRECATED,
        new ConvertToExplicitSymbolQuickFix(element)
      )
    }
  }
}
