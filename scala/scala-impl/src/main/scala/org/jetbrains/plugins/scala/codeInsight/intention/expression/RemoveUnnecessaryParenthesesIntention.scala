package org.jetbrains.plugins.scala.codeInsight.intention.expression

import consulo.annotation.component.ExtensionImpl
import consulo.language.editor.intention.IntentionMetaData

import consulo.language.editor.rawHighlight.HighlightDisplayKey
import consulo.language.editor.intention.PsiElementBaseIntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils
import consulo.codeEditor.Editor
import consulo.project.Project
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager
import consulo.language.psi.PsiElement
import consulo.language.psi.util.PsiTreeUtil
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.extensions.ParenthesizedElement.Ops
import org.jetbrains.plugins.scala.lang.psi.api.base.ScParenthesizedElement

@ExtensionImpl
@IntentionMetaData(ignoreId = "scala.RemoveUnnecessaryParenthesesIntention", fileExtensions = Array("scala"), categories = Array("Scala", "Expressions"))
class RemoveUnnecessaryParenthesesIntention extends PsiElementBaseIntentionAction {
  override def getFamilyName: String = ScalaBundle.message("remove.unnecessary.parentheses")

  override def getText: String = ScalaBundle.message("remove.unnecessary.parentheses")

  override def isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean = {
    !isInspectionEnabledIn(project, "ScalaUnnecessaryParentheses") &&
      Option(PsiTreeUtil.getParentOfType(element, classOf[ScParenthesizedElement], false))
        .exists(_.isParenthesisRedundant)
  }

  private def isInspectionEnabledIn(project: Project, id: String) =
    Option(HighlightDisplayKey.find(id)).exists(
      InspectionProjectProfileManager.getInstance(project).getCurrentProfile.isToolEnabled)

  override def invoke(project: Project, editor: Editor, element: PsiElement): Unit =
    Option(PsiTreeUtil.getParentOfType(element, classOf[ScParenthesizedElement])).foreach {
      case expr if expr.isNestedParenthesis => invoke(project, editor, expr)
      case expr if expr.isParenthesisRedundant =>
        IntentionPreviewUtils.write { () =>
          expr.doStripParentheses()
        }
      case _ =>
    }
}
