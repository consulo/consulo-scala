package org.jetbrains.plugins.scala.lang.psi.stubs


import org.jetbrains.plugins.scala.api.lang.psi.stubs.ScStubElement
import consulo.language.psi.stub.NamedStub
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction
import org.jetbrains.plugins.scala.lang.psi.stubs.impl.{ScExpressionOwnerStub, ScTypeElementOwnerStub}

trait ScFunctionStub[F <: ScFunction] extends NamedStub[F] with ScStubElement[F]
  with ScTopLevelElementStub[F]
  with ScMemberOrLocal[F]
  with ScTypeElementOwnerStub[F]
  with ScExpressionOwnerStub[F]
  with ScImplicitStub[F]
  with ScGivenStub {

  def implicitConversionParameterClass: Option[String]

  def isDeclaration: Boolean

  def annotations: Array[String]

  def hasAssign: Boolean

  def isExtensionMethod: Boolean
}
