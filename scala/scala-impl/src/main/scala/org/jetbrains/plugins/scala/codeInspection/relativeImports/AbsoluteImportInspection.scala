package org.jetbrains.plugins.scala.codeInspection.relativeImports

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.localize.LocalizeValue
import consulo.java.analysis.impl.localize.JavaQuickFixLocalize
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import consulo.language.editor.inspection.{LocalInspectionTool, LocalQuickFix, ProblemDescriptor, ProblemHighlightType, ProblemsHolder}
import consulo.project.Project
import consulo.document.util.TextRange
import consulo.language.psi.PsiElementVisitor
import org.jetbrains.plugins.scala.codeInspection.relativeImports.AbsoluteImportInspection.OptimizeImportsQuickFix
import org.jetbrains.plugins.scala.codeInspection.{PsiElementVisitorSimple, ScalaInspectionBundle}
import org.jetbrains.plugins.scala.editor.importOptimizer.{OptimizeImportSettings, ScalaImportOptimizer}
import org.jetbrains.plugins.scala.extensions.{ObjectExt, PsiElementExt}
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports.ScImportExpr

@ExtensionImpl
class AbsoluteImportInspection extends LocalInspectionTool {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("display.name.absolute.import"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.general"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case importExpr@ScImportExpr.qualifier(qualifier) =>

      importExpr.containingFile.foreach { file =>
        OptimizeImportSettings(file).basePackage.foreach { basePackage =>
          if ((qualifier.getText + ".").startsWith(basePackage + ".")) {
            holder.registerProblem(importExpr, ScalaInspectionBundle.message("absolute.import.detected"),
              ProblemHighlightType.GENERIC_ERROR_OR_WARNING, TextRange.create(0, basePackage.length + 1), new OptimizeImportsQuickFix())
          }
        }
      }
    case _ =>
  }
}

private object AbsoluteImportInspection {
  private class OptimizeImportsQuickFix extends LocalQuickFix {
    override def getName: LocalizeValue = JavaQuickFixLocalize.optimizeImportsFix()

    override def applyFix(project: Project, descriptor: ProblemDescriptor): Unit = {
      descriptor.getPsiElement.getContainingFile.asOptionOf[ScalaFile].foreach { file =>
        ScalaImportOptimizer.findOptimizerFor(file).foreach { optimizer =>
          optimizer.processFile(file).run()
        }
      }
    }

    override def generatePreview(project: Project, previewDescriptor: ProblemDescriptor): IntentionPreviewInfo =
      IntentionPreviewInfo.EMPTY
  }
}
