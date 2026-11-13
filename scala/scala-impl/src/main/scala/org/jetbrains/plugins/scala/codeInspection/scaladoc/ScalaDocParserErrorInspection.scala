package org.jetbrains.plugins.scala.codeInspection.scaladoc

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle

import consulo.language.editor.inspection.{LocalInspectionTool, ProblemHighlightType, ProblemsHolder}
import consulo.application.dumb.DumbAware
import consulo.language.psi.{PsiElementVisitor, PsiErrorElement}
import org.jetbrains.plugins.scala.extensions.PsiElementExt
import org.jetbrains.plugins.scala.incremental.Highlighting._
import org.jetbrains.plugins.scala.lang.psi.api.{ScalaElementVisitor, ScalaPsiElement}
import org.jetbrains.plugins.scala.lang.scaladoc.psi.api.ScDocComment

@ExtensionImpl
final class ScalaDocParserErrorInspection extends LocalInspectionTool with DumbAware {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.wrong.scaladoc.element"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.scaladoc"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getShortName: String = "ScalaDocParserErrorInspection"

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def isEnabledByDefault: Boolean = true

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
    new ScalaElementVisitor {
      override def visitDocComment(s: ScDocComment): Unit =
        visitScaladocElement(s)

      override def visitScaladocElement(element: ScalaPsiElement): Unit = {
        if (!element.isVisible(holder.getProject, holder.getFile)) return

        element.getChildren.foreach {
          case errorElement: PsiErrorElement =>
            val markedElement =
              if (element.getTextLength > 0) element
              else element.nextLeafs.find(_.getTextLength > 0).get
            holder.registerProblem(
              holder.getManager.createProblemDescriptor(
                markedElement,
                errorElement.getErrorDescription,
                isOnTheFly,
                null,
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
              )
            );
          case b: ScalaPsiElement if b.getChildren.nonEmpty => visitScaladocElement(b)
          case _ => //do nothing
        }
      }
    }
}
