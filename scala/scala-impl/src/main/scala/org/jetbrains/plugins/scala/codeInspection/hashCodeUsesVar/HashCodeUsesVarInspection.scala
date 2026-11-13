package org.jetbrains.plugins.scala.codeInspection.hashCodeUsesVar

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.inspection.{LocalInspectionTool, ProblemsHolder}
import consulo.language.psi.PsiElementVisitor
import com.intellij.java.language.psi.PsiMethod
import com.siyeh.ig.psiutils.MethodUtils
import org.jetbrains.plugins.scala.codeInspection.{PsiElementVisitorSimple, ScalaInspectionBundle}
import org.jetbrains.plugins.scala.lang.psi.api.ScalaRecursiveElementVisitor
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScReferencePattern
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScReferenceExpression
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScVariable

@ExtensionImpl
class HashCodeUsesVarInspection extends LocalInspectionTool {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.non.value.field.is.accessed.in.hashcode"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.general"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WEAK_WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE


  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case hashCodeMethod: PsiMethod if MethodUtils.isHashCode(hashCodeMethod) =>
      hashCodeMethod.accept(new ScalaRecursiveElementVisitor {
        override def visitReferenceExpression(exp: ScReferenceExpression): Unit = {
          super.visitReferenceExpression(exp)
          exp.resolve() match {
            case field: ScReferencePattern =>
              field.nameContext match {
                case variable: ScVariable if variable.isDefinedInClass =>
                  holder.registerProblem(exp, ScalaInspectionBundle.message("displayname.non.value.field.is.accessed.in.hashcode"))
                case _ =>
              }
            case _ =>
          }
        }
      })
    case _ =>
  }
}
