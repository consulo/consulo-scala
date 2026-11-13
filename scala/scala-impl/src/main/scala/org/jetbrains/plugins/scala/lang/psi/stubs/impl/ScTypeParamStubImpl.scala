package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.stub.IStubElementType
import consulo.language.psi.PsiElement
import consulo.language.psi.stub.StubElement
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScTypeParam
import org.jetbrains.plugins.scala.lang.psi.stubs.ScTypeParamStub

class ScTypeParamStubImpl(
  parent: StubElement[? <: PsiElement],
  elementType: IStubElementType[?, ?],
  name: String,
  override val text: String,
  override val lowerBoundText: Option[String],
  override val upperBoundText: Option[String],
  override val viewBoundsTexts: Array[String],
  override val contextBoundsTexts: Array[String],
  override val isCovariant: Boolean,
  override val isContravariant: Boolean,
  override val containingFileName: String
) extends ScNamedStubBase[ScTypeParam](parent, elementType, name)
  with ScTypeParamStub
  with ScBoundsOwnerStub[ScTypeParam]
