package org.jetbrains.plugins.scala.codeInspection.targetNameAnnotation

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import com.intellij.java.analysis.impl.codeInspection.RemoveAnnotationQuickFix
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScEnumCases, ScValueOrVariableDefinition}

@ExtensionImpl(id = "MultipleTargetsTargetName")
class MultipleTargetsTargetNameInspection extends TargetNameInspectionBase {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.multiple.targets.targetname"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.general"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.ERROR

  override def getLanguage: Language = ScalaLanguage.INSTANCE


  import MultipleTargetsTargetNameInspection._

  override protected val findProblemElement: PartialFunction[PsiElement, ProblemElement] = {
    case TargetNameAnnotationWithOwner(annotation, annotationOwner@MultipleTargets()) =>
      val quickFix = new RemoveAnnotationQuickFix(annotation, annotationOwner)
      ProblemElement(annotation, quickFix, message)
  }
}

object MultipleTargetsTargetNameInspection {
  private[targetNameAnnotation] val message = ScalaInspectionBundle.message("targetname.multiple.targets")

  private object MultipleTargets {
    def unapply(element: PsiElement): Boolean = element match {
      case valOrVar: ScValueOrVariableDefinition =>
        valOrVar.bindings.sizeIs > 1
      case enumCases: ScEnumCases =>
        enumCases.declaredElements.sizeIs > 1
      case _ => false
    }
  }
}
