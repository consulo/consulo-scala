package org.jetbrains.plugins.scala.codeInspection.targetNameAnnotation

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import com.intellij.java.analysis.impl.codeInspection.RemoveAnnotationQuickFix
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle

@ExtensionImpl(id = "MultipleTargetNameAnnotations")
class MultipleTargetNameAnnotationsInspection extends TargetNameInspectionBase {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.multiple.targetname.annotations"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.general"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override protected val findProblemElement: PartialFunction[PsiElement, ProblemElement] = {
    case TargetNameAnnotationWithOwner(annotation, owner) if owner.annotations(TargetNameAnnotationFQN).sizeIs > 1 =>
      val quickFix = new RemoveAnnotationQuickFix(annotation, owner)
      ProblemElement(annotation, quickFix, MultipleTargetNameAnnotationsInspection.message)
  }
}

object MultipleTargetNameAnnotationsInspection {
  private[targetNameAnnotation] val message =
    ScalaInspectionBundle.message("all.but.last.targetname.annotation.ignored")
}
