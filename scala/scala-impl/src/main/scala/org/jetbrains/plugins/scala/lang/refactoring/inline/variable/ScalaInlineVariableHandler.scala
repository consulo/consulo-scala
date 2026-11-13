package org.jetbrains.plugins.scala.lang.refactoring.inline.variable

import consulo.annotation.component.ExtensionImpl

import consulo.codeEditor.Editor
import consulo.project.Project
import consulo.language.psi.util.PsiTreeUtil
import com.intellij.java.impl.refactoring.HelpID
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.extensions.ObjectExt
import org.jetbrains.plugins.scala.lang.psi.api.ScalaPsiElement
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScBindingPattern
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScPatternDefinition, ScValueOrVariableDefinition, ScVariableDefinition}
import org.jetbrains.plugins.scala.lang.refactoring.inline.ScalaInlineActionHandler

@ExtensionImpl(id = "scalaInlineVariableHandler")
final class ScalaInlineVariableHandler extends ScalaInlineActionHandler {
  override protected val helpId: String = ScalaInlineVariableHandler.HelpId
  override protected val refactoringName: String = ScalaInlineVariableHandler.RefactoringName

  override protected def canInlineScalaElement(element: ScalaPsiElement): Boolean = element.is[ScBindingPattern]

  override protected def inlineScalaElement(element: ScalaPsiElement)(implicit project: Project, editor: Editor): Unit = element match {
    case pattern: ScBindingPattern =>
      PsiTreeUtil.getParentOfType(pattern, classOf[ScValueOrVariableDefinition]) match {
        case definition: ScPatternDefinition if !definition.isSimple =>
          showErrorHint(ScalaBundle.message("cannot.inline.not.simple.definition", "value"))
        case definition: ScVariableDefinition if !definition.isSimple =>
          showErrorHint(ScalaBundle.message("cannot.inline.not.simple.definition", "variable"))
        case parent if parent != null && parent.declaredElements == Seq(element) =>
          if (validateReferences(pattern)) {
            val dialog = new ScalaInlineVariableDialog(pattern, parent)
            showDialog(dialog)
          }
        case _ =>
          showErrorHint(ScalaBundle.message("cannot.inline.not.simple.definition", "pattern"))
      }
    case _ =>
  }
}

object ScalaInlineVariableHandler {
  val HelpId: String = HelpID.INLINE_VARIABLE

  // upstream: @NlsContexts.DialogTitle
  val RefactoringName: String = ScalaBundle.message("inline.variable.title")
}
