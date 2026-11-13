package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.stub.IStubElementType
import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{StubBase, StubElement}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.templates.ScExtendsBlock
import org.jetbrains.plugins.scala.lang.psi.stubs.ScExtendsBlockStub

import scala.collection.immutable.ArraySeq

class ScExtendsBlockStubImpl(parent: StubElement[? <: PsiElement],
                             elementType: IStubElementType[?, ?],
                             override val baseClasses: ArraySeq[String])
  extends StubBase[ScExtendsBlock](parent, elementType) with ScExtendsBlockStub
