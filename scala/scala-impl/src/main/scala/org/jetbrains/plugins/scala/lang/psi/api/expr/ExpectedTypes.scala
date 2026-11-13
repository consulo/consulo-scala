package org.jetbrains.plugins.scala.lang.psi.api.expr

import consulo.application.ApplicationManager
import consulo.annotation.component.{ComponentScope, ServiceAPI}
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScTypeElement
import org.jetbrains.plugins.scala.lang.psi.api.expr.ExpectedTypes.ParameterType
import org.jetbrains.plugins.scala.lang.psi.types.ScType

@ServiceAPI(ComponentScope.APPLICATION)
trait ExpectedTypes {
  def smartExpectedType(expr: ScExpression, fromUnderscore: Boolean = true): Option[ScType]

  def expectedExprType(expr: ScExpression, fromUnderscore: Boolean = true): Option[ParameterType]

  def expectedExprTypes(expr: ScExpression,
                        withResolvedFunction: Boolean = false,
                        fromUnderscore: Boolean = true): Array[ParameterType]
}

object ExpectedTypes {
  type ParameterType = (ScType, Option[ScTypeElement])

  def instance(): ExpectedTypes = ApplicationManager.getApplication.getInstance(classOf[ExpectedTypes])
}