package org.jetbrains.plugins.scala.codeInspection

import consulo.localize.LocalizeValue
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils
import consulo.language.editor.inspection.LocalQuickFixOnPsiElement
import consulo.project.Project
import consulo.language.psi.{PsiElement, PsiFile}
import org.jetbrains.annotations.{Nls, NotNull}
import org.jetbrains.plugins.scala.extensions.ValidSmartPointer

/**
 * The purpose of this class is to avoid holding references to instances of PsiElement in a quickfix.
 * Quickfix may be invoked after reparsing of the file so psiElements which existed when quickfix was created may
 * be invalidated.
 */
abstract class AbstractFixOnPsiElement[T <: PsiElement](@Nls name: String, element: T)
  extends LocalQuickFixOnPsiElement(element) {

  override def getText: LocalizeValue = LocalizeValue.of(name)

  def getFamilyName: String = name

  override final def invoke(
    @NotNull project: Project,
    @NotNull file: PsiFile,
    @NotNull startElement: PsiElement,
    @NotNull endElement: PsiElement
  ): Unit = {
    if (!IntentionPreviewUtils.prepareElementForWrite(file))
      return

    if (!startElement.isValid)
      return

    doApplyFix(startElement.asInstanceOf[T])(using project)
  }

  protected def doApplyFix(element: T)
                          (implicit project: Project): Unit
}

abstract class AbstractFixOnTwoPsiElements[T <: PsiElement, S <: PsiElement](@Nls name: String,
                                                                             startElement: T,
                                                                             endElement: S)
  extends LocalQuickFixOnPsiElement(startElement, endElement) {

  override def getText: LocalizeValue = LocalizeValue.of(name)

  def getFamilyName: String = name

  override final def invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement): Unit = {
    if (!IntentionPreviewUtils.prepareElementForWrite(file)) return

    myStartElement match {
      case ValidSmartPointer(first: T @unchecked) =>
        myEndElement match {
          // myEndElement is null when start and end elements are equal in LocalQuickFixOnPsiElement's constructor
          case null => doApplyFix(first, first.asInstanceOf[S])(using project)
          case ValidSmartPointer(second: S @unchecked) => doApplyFix(first, second)(using project)
          case _ =>
        }
      case _ =>
    }
  }

  protected def doApplyFix(first: T, second: S)
                          (implicit project: Project): Unit
}
