package org.jetbrains.plugins.scala.annotator.quickfix

import consulo.language.editor.intention.SyntheticIntentionAction
import consulo.localize.LocalizeValue
import consulo.language.editor.util.LanguageUndoUtil
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils
import consulo.language.editor.inspection.FileModifier
import consulo.language.editor.intention.IntentionAction
import consulo.codeEditor.Editor
import consulo.project.Project
import consulo.language.psi.PsiFile
import consulo.language.psi.util.PsiTreeUtil
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScTypeElement
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createTypeElementFromText
import org.jetbrains.plugins.scala.lang.psi.types.api.presentation.TypePresentation
import org.jetbrains.plugins.scala.lang.psi.types.{Context, ScType, ScTypeExt, TypePresentationContext}
import org.jetbrains.plugins.scala.project.ProjectContext

/**
 * @param widenNewType true to replace singleton types with wider version, e.g. use String instead of "42" (see SCL-16489)
 */
final class ChangeTypeFix(
  typeElement: ScTypeElement,
  newTypeOriginal: ScType,
  widenNewType: Boolean = true
) extends SyntheticIntentionAction {

  private lazy val newType: ScType =
    if (widenNewType) newTypeOriginal.widen
    else newTypeOriginal

  override val getText: LocalizeValue = LocalizeValue.of {
    implicit val tpc: TypePresentationContext = TypePresentationContext(typeElement)
    val (oldTypeDescription, newTypeDescription) = typeElement.`type`() match {
      case Right(oldType) => TypePresentation.different(oldType, newType)
      case _ => (typeElement.getText, newType.presentableText)
    }
    ScalaBundle.message("change.type.to", oldTypeDescription, newTypeDescription)
  }

  def getFamilyName: String = ScalaBundle.message("family.name.change.type")

  override def startInWriteAction: Boolean = true

  override def isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean =
    typeElement.isValid

  override def invoke(project: Project, editor: Editor, file: PsiFile): Unit = {
    if (!typeElement.isValid) return
    if (!IntentionPreviewUtils.prepareElementForWrite(typeElement.getContainingFile)) return
    if (typeElement.getParent == null || typeElement.getParent.getNode == null) return

    implicit val projectContext: ProjectContext = ProjectContext.fromPsi(typeElement)
    implicit val context: Context = Context(typeElement)

    val replaced = typeElement.replace(createTypeElementFromText(newType.canonicalText(TypePresentationContext(typeElement)), typeElement))
    ScalaPsiUtil.adjustTypes(replaced)
    LanguageUndoUtil.markPsiFileForUndo(file)
  }

  override def getFileModifierForPreview(target: PsiFile): FileModifier =
    new ChangeTypeFix(PsiTreeUtil.findSameElementInCopy(typeElement, target), newType)
}
