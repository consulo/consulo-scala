package org.jetbrains.plugins.scala.codeInspection.targetNameAnnotation

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.extensions.{ObjectExt, PsiElementExt}
import org.jetbrains.plugins.scala.lang.psi.api.base.ScAnnotation
import org.jetbrains.plugins.scala.lang.psi.api.base.literals.ScStringLiteral
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScArgumentExprList

@ExtensionImpl(id = "EmptyTargetName")
class EmptyTargetNameInspection extends TargetNameInspectionBase {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.empty.targetname"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.general"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.ERROR

  override def getLanguage: Language = ScalaLanguage.INSTANCE


  import EmptyTargetNameInspection._

  override protected val findProblemElement: PartialFunction[PsiElement, ProblemElement] = {
    case TargetNameArgument(extName) if extName.getValue.isEmpty =>
      ProblemElement(extName, description = message)
  }
}

object EmptyTargetNameInspection {
  private[targetNameAnnotation] val message = ScalaInspectionBundle.message("targetname.cannot.be.empty")

  object TargetNameArgument {
    def unapply(extName: ScStringLiteral): Option[ScStringLiteral] =
      if (extName.getParent.is[ScArgumentExprList]) {
        extName.parentOfType[ScAnnotation].collect {
          case annotation if annotation.hasQualifiedName(TargetNameAnnotationFQN) => extName
        }
      } else None
  }
}
