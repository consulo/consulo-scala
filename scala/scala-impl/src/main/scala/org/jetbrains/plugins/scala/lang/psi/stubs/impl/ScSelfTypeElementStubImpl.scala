package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.stub.IStubElementType
import consulo.language.psi.PsiElement
import consulo.language.psi.stub.StubElement
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScSelfTypeElement
import org.jetbrains.plugins.scala.lang.psi.stubs.ScSelfTypeElementStub

class ScSelfTypeElementStubImpl(parent: StubElement[? <: PsiElement],
                                elementType: IStubElementType[?, ?],
                                @Nullable name: String,
                                override val typeText: Option[String],
                                override val classNames: Array[String])
  extends ScNamedStubBase[ScSelfTypeElement](parent, elementType, name) with ScSelfTypeElementStub
