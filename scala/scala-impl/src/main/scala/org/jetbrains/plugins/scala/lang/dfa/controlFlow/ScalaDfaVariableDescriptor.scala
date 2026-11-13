package org.jetbrains.plugins.scala.lang.dfa.controlFlow

import com.intellij.java.language.codeInsight.Nullability
import com.intellij.java.analysis.impl.codeInspection.dataFlow.jvm.descriptors.JvmVariableDescriptor
import consulo.language.dataFlow.types.DfType
import consulo.language.dataFlow.value.DfaVariableValue
import consulo.language.psi.{PsiElement, PsiNamedElement}
import com.intellij.java.language.psi.PsiModifierListOwner
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.dfa.utils.ScalaDfaTypeUtils.{elementNullabilityForRead, isStableElement, scTypeToDfType}
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScReferenceExpression
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction.CommonNames
import org.jetbrains.plugins.scala.lang.psi.types.result.Typeable


case class ScalaDfaVariableDescriptor(variable: PsiElement,
                                      qualifier: Option[ScalaDfaVariableDescriptor],
                                      override val isStable: Boolean) extends JvmVariableDescriptor {

  override def toString: String = (qualifier, variable) match {
    case (Some(qualifierVariable), namedElement: PsiNamedElement) =>
      s"$qualifierVariable.${namedElement.name}"
    case (None, namedElement: PsiNamedElement) => namedElement.name
    case _ => "<unknown>"
  }

  override def getDfType(qualifier: DfaVariableValue): DfType = dfType

  def dfType: DfType = variable match {
    case typeable: Typeable =>
      val scType = typeable.`type`().getOrAny
      val nullability = variable match {
        case owner: PsiModifierListOwner =>
          elementNullabilityForRead(scType, Some(owner))
        case _ => Nullability.UNKNOWN
      }
      scTypeToDfType(scType, nullability)
    case _ => DfType.TOP
  }
}

object ScalaDfaVariableDescriptor {

  def fromReferenceExpression(expression: ScReferenceExpression): Option[ScalaDfaVariableDescriptor] = {
    val qualifierVariable = expression.qualifier match {
      case Some(reference: ScReferenceExpression) => fromReferenceExpression(reference)
      case _ => None
    }

    if (expression.isQualified && qualifierVariable.isEmpty) None
    else expression.getReference.bind()
      .map(srr => if (srr.name == CommonNames.Apply) srr.getActualElement else srr.element)
      .map(element => ScalaDfaVariableDescriptor(element, qualifierVariable,
        isStableElement(element) && qualifierVariable.forall(qualifier => isStableElement(qualifier.variable))))
  }
}
