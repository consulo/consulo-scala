package org.jetbrains.plugins.scala.codeInspection.notImplementedCode

import consulo.language.editor.template.TemplateBuilder
import consulo.language.editor.template.TemplateBuilderFactory
import consulo.language.editor.CodeInsightUtilCore
import consulo.language.editor.inspection.{LocalInspectionTool, ProblemsHolder}
import consulo.project.Project
import consulo.language.psi.{PsiElement, PsiElementVisitor}
import org.jetbrains.plugins.scala.annotator.TemplateUtils
import org.jetbrains.plugins.scala.codeInspection.{AbstractFixOnPsiElement, PsiElementVisitorSimple, ScalaInspectionBundle}
import org.jetbrains.plugins.scala.extensions._

class NotImplementedCodeInspection extends LocalInspectionTool {

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case reference @ ReferenceTarget(Member("???", "scala.Predef")) =>
      holder.registerProblem(reference, ScalaInspectionBundle.message("not.implemented"), new ImplementQuickFix(reference))
    case _ =>
  }

  private class ImplementQuickFix(e: PsiElement) extends AbstractFixOnPsiElement(ScalaInspectionBundle.message("implement.quickfix.name"), e) {

    override protected def doApplyFix(elem: PsiElement)
                                     (implicit project: Project): Unit = {
      val builder = TemplateBuilderFactory.getInstance().createTemplateBuilder(elem)
      builder.replaceElement(elem, elem.getText)
      CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(elem)
      val template = builder.buildTemplate()
      //NOTE: unfortunately we don't have access to the original editor in this quick fix
      // Thus when the quick fix is invoked in Scala REPL, a new editor will be opened (like in SCL-3750)
      TemplateUtils.positionCursorAndStartTemplate(elem, template, originalEditor = None)
    }
  }
}
