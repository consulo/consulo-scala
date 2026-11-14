package org.jetbrains.sbt.language.completion

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.AutoPopupController
import consulo.language.editor.completion.{CompletionConfidence, CompletionType}
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

@ExtensionImpl(order = "before scalaCompletionConfidence, before EnableAutoPopupInScalaDirectiveComment, before enableAutopopupInHttpHeaderReferences, before enableAutopopupInUrlReferences")
class EnableAutoPopupInScalacOptionsStrings extends CompletionConfidence {
  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def shouldSkipAutopopup(contextElement: PsiElement, psiFile: PsiFile, offset: Int): ThreeState =
    withScalacOption(contextElement)(onMismatch = ThreeState.UNSURE, onMatch = _ => ThreeState.NO)
}

@ExtensionImpl(order = "before selectionQuoting, before space, before QuotesTypedHandlerAutoPopup, before SlashTypedHandlerAutoPopup, before java, before javadoc, before ScalaAutoPopupCompletionHandler")
final class ScalacOptionsAutoPopupCompletionHandler extends TypedHandlerDelegate {

  import ScalacOptionsAutoPopupCompletionHandler._

  override def checkAutoPopup(charTyped: Char, project: Project, editor: Editor, file: PsiFile): Result =
    if (!file.is[ScalaFile, SbtFileImpl] || !(charTyped == '"' || charTyped == '-')) super.checkAutoPopup(charTyped, project, editor, file)
    else {
      val offset = editor.getCaretModel.getOffset
      AutoPopupController.getInstance(project).scheduleAutoPopup(editor, CompletionType.BASIC, (file: PsiFile) => condition(offset)(file))
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
