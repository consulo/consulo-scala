package org.jetbrains.sbt.language.completion

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.AutoPopupController
import consulo.language.editor.completion.CompletionConfidence
import consulo.language.editor.action.TypedHandlerDelegate
import consulo.language.editor.action.TypedHandlerDelegate.Result
import consulo.codeEditor.Editor
import consulo.project.Project
import consulo.language.psi.{PsiElement, PsiFile}
import consulo.util.lang.ThreeState
import org.jetbrains.plugins.scala.extensions.ObjectExt
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.base.literals.ScStringLiteral
import org.jetbrains.sbt.language.SbtFileImpl
import org.jetbrains.sbt.language.utils.SbtScalacOptionUtils.withScalacOption

@ExtensionImpl(order = "first")
class EnableAutoPopupInScalacOptionsStrings extends CompletionConfidence {
  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def shouldSkipAutopopup(editor: Editor, contextElement: PsiElement, psiFile: PsiFile, offset: Int): ThreeState =
    withScalacOption(contextElement)(onMismatch = ThreeState.UNSURE, onMatch = _ => ThreeState.NO)
}

@ExtensionImpl(order = "first")
final class ScalacOptionsAutoPopupCompletionHandler extends TypedHandlerDelegate {

  import ScalacOptionsAutoPopupCompletionHandler._

  override def checkAutoPopup(charTyped: Char, project: Project, editor: Editor, file: PsiFile): Result =
    if (!file.is[ScalaFile, SbtFileImpl] || !(charTyped == '"' || charTyped == '-')) super.checkAutoPopup(charTyped, project, editor, file)
    else {
      val offset = editor.getCaretModel.getOffset
      AutoPopupController.getInstance(project).scheduleAutoPopup(editor, condition(offset)(_))
      Result.STOP
    }
}

object ScalacOptionsAutoPopupCompletionHandler {
  // "", "-" or "--"
  private def check(str: ScStringLiteral): Boolean = str.getValue.length < 3 && str.getValue.forall(_ == '-')

  private def condition(offset: Int)(file: PsiFile): Boolean = {
    val leaf = file.findElementAt(offset)
    leaf != null && withScalacOption(leaf)(onMatch = check, onMismatch = false)
  }
}
