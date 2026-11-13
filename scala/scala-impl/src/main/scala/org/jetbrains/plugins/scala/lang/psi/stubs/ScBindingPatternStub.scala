package org.jetbrains.plugins.scala.lang.psi.stubs


import org.jetbrains.plugins.scala.api.lang.psi.stubs.ScStubElement
import consulo.language.psi.stub.NamedStub
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScBindingPattern

trait ScBindingPatternStub[P <: ScBindingPattern] extends NamedStub[P] with ScStubElement[P]
