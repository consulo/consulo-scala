package org.jetbrains.plugins.scala.lang.psi.stubs


import org.jetbrains.plugins.scala.api.lang.psi.stubs.ScStubElement
import consulo.language.psi.stub.NamedStub
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScSelfTypeElement
import org.jetbrains.plugins.scala.lang.psi.stubs.impl.ScTypeElementOwnerStub

trait ScSelfTypeElementStub extends NamedStub[ScSelfTypeElement] with ScStubElement[ScSelfTypeElement] with ScTypeElementOwnerStub[ScSelfTypeElement] {
  def classNames: Array[String]
}