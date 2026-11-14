package org.jetbrains.sbt
package codeInspection

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.sbt.SbtBundle

import consulo.language.editor.inspection.{LocalInspectionTool, ProblemsHolder}
import consulo.application.dumb.DumbAware
import consulo.project.Project
import consulo.language.psi.PsiElementVisitor
import org.jetbrains.plugins.scala.codeInspection.{AbstractFixOnPsiElement, PsiElementVisitorSimple}
import org.jetbrains.plugins.scala.lang.psi.api.ScalaRecursiveElementVisitor
import org.jetbrains.plugins.scala.lang.psi.api.base.literals.ScStringLiteral
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScReferencePattern
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScMethodCall
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScPatternDefinition
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createExpressionFromText
import org.jetbrains.plugins.scala.project.ScalaFeatures

@ExtensionImpl
final class SbtReplaceProjectWithProjectInInspection extends LocalInspectionTool with DumbAware {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(SbtBundle.message("displayname.replace.project.with.project.in"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(SbtBundle.message("group.sbt"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case defn: ScPatternDefinition if defn.getContainingFile.getFileType.getName == Sbt.Name =>
      (defn.expr, defn.bindings) match {
        case (Some(call: ScMethodCall), Seq(projectNamePattern: ScReferencePattern)) =>
          findPlaceToFix(call, projectNamePattern.getText).foreach { place =>
            holder.registerProblem(place, SbtBundle.message("sbt.inspection.projectIn.name"), new SbtReplaceProjectWithProjectInQuickFix(place))
          }
        case _ => // do nothing
      }
    case _ =>
  }

  private def findPlaceToFix(call: ScMethodCall, projectName: String): Option[ScMethodCall] = {
    var placeToFix: Option[ScMethodCall] = None
    val visitor = new ScalaRecursiveElementVisitor {
      override def visitMethodCallExpression(call: ScMethodCall): Unit = call match {
        case ScMethodCall(expr, Seq(ScStringLiteral(name), _))
          if expr.textMatches("Project") && name == projectName =>
          placeToFix = Some(call)
        case _ =>
          super.visitMethodCallExpression(call)
      }
    }
    call.accept(visitor)
    placeToFix
  }
}

final class SbtReplaceProjectWithProjectInQuickFix(call: ScMethodCall)
  extends AbstractFixOnPsiElement(SbtBundle.message("sbt.inspection.projectIn.name"), call)
    with DumbAware {
  override protected def doApplyFix(element: ScMethodCall)
                                   (implicit project: Project): Unit = {
    element match {
      case ScMethodCall(_, Seq(_, pathElt)) =>
        element.replace(createExpressionFromText("project.in(" + pathElt.getText + ")", ScalaFeatures.default))
      case _ => // do nothing
    }
  }
}
