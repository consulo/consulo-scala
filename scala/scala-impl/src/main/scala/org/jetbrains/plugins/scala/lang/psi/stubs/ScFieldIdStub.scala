package org.jetbrains.plugins.scala.lang.psi.stubs


import org.jetbrains.plugins.scala.api.lang.psi.stubs.ScStubElement
import consulo.language.psi.stub.NamedStub
import org.jetbrains.plugins.scala.lang.psi.api.base.ScFieldId

trait ScFieldIdStub extends NamedStub[ScFieldId] with ScStubElement[ScFieldId]