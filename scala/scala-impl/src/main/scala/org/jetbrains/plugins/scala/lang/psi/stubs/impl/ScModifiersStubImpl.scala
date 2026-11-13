package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.stub.IStubElementType
import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{StubBase, StubElement}
import org.jetbrains.plugins.scala.lang.lexer.ScalaModifier
import org.jetbrains.plugins.scala.lang.psi.api.base.ScModifierList
import org.jetbrains.plugins.scala.lang.psi.stubs.ScModifiersStub
import org.jetbrains.plugins.scala.util.EnumSet.EnumSet

class ScModifiersStubImpl(parent: StubElement[? <: PsiElement],
                          elemType: IStubElementType[?, ?],
                          override val modifiers: EnumSet[ScalaModifier])
  extends StubBase[ScModifierList](parent, elemType) with ScModifiersStub
