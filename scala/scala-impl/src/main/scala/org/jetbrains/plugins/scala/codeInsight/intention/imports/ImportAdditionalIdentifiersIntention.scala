package org.jetbrains.plugins.scala.codeInsight.intention.imports

import consulo.localize.LocalizeValue

import consulo.annotation.component.ExtensionImpl
import consulo.language.editor.intention.IntentionMetaData

import consulo.language.editor.intention.PsiElementBaseIntentionAction
import com.intellij.codeInsight.intention.preview.{IntentionPreviewInfo, IntentionPreviewUtils}
import consulo.codeEditor.Editor
import consulo.project.Project
import consulo.language.psi.{PsiDocumentManager, PsiElement, PsiFile, PsiWhiteSpace}
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.api.base.ScStableCodeReference
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports.ScImportExpr
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory

import scala.annotation.tailrec

/**
  * Jason Zaugg
  */

@ExtensionImpl
@IntentionMetaData(ignoreId = "scala.ImportAdditionalIdentifiersIntention", fileExtensions = Array("scala"), categories = Array("Scala", "Import"))
class ImportAdditionalIdentifiersIntention extends PsiElementBaseIntentionAction {
  def getFamilyName: String = ScalaBundle.message("family.name.import.additional.identifiers")

  override def getText: LocalizeValue = LocalizeValue.of(ScalaBundle.message("import.additional.identifiers.from.qualifier"))

  override def isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean =
    check(project, editor, element).isDefined

  override def invoke(project: Project, editor: Editor, element: PsiElement): Unit =
    if (element.isValid) check(project, editor, element).foreach(_.apply())

  override def startInWriteAction(): Boolean = false

  def generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo = {
    val element = file.findElementAt(editor.getCaretModel.getOffset)
    check(project, editor, element) match {
      case Some(action) =>
        action()
        IntentionPreviewInfo.DIFF
      case None => IntentionPreviewInfo.EMPTY
    }
  }

  @tailrec
  private def check(project: Project, editor: Editor, element: PsiElement): Option[() => Unit] = {
    element match {
      case _: PsiWhiteSpace if element.getPrevSibling != null &&
        editor.getCaretModel.getOffset == element.getPrevSibling.getTextRange.getEndOffset =>
        val prev = element.getContainingFile.findElementAt(element.getPrevSibling.getTextRange.getEndOffset - 1)
        check(project, editor, prev)
      case null => None
      case ChildOf(id: ScStableCodeReference) if id.nameId == element =>
        id.getParent match {
          case imp@ScImportExpr.qualifier(qualifier) if imp.selectorSet.isEmpty =>
            val doIt = () => {
              val name = s"${qualifier.getText}.{${id.nameId.getText}}"

              IntentionPreviewUtils.write { () =>
                val replaced = imp.replace(ScalaPsiElementFactory.createImportExprFromText(name, element))
                val document = editor.getDocument
                val documentManager = PsiDocumentManager.getInstance(project)
                documentManager.doPostponedOperationsAndUnblockDocument(document)
                documentManager.commitDocument(document)
                document.insertString(replaced.getTextRange.getEndOffset - 1, ", ")
                editor.getCaretModel.moveToOffset(replaced.getTextRange.getEndOffset + 1)
              }
            }
            Some(doIt)
          case _ => None
        }
      case _ => None
    }
  }
}
