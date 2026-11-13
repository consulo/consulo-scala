package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{StubBase, StubElement}
import consulo.language.ast.IElementType
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.templates.ScExtendsBlock
import org.jetbrains.plugins.scala.lang.psi.stubs.ScExtendsBlockStub

import scala.collection.immutable.ArraySeq

class ScExtendsBlockStubImpl(parent: StubElement[? <: PsiElement],
                             elementType: IElementType,
                             override val baseClasses: ArraySeq[String])
  extends StubBase[ScExtendsBlock](parent, elementType) with ScExtendsBlockStub
