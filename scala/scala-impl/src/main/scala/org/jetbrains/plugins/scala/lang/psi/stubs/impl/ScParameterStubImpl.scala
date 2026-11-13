package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.stub.IStubElementType
import consulo.language.psi.PsiElement
import consulo.language.psi.stub.StubElement
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameter
import org.jetbrains.plugins.scala.lang.psi.stubs.ScParameterStub

class ScParameterStubImpl(
  parent: StubElement[? <: PsiElement],
  elementType: IStubElementType[?, ?],
  name: String,
  override val typeText: Option[String],
  override val isStable: Boolean,
  override val isDefaultParameter: Boolean,
  override val isRepeated: Boolean,
  override val isVal: Boolean,
  override val isVar: Boolean,
  override val isCallByNameParameter: Boolean,
  override val isAnonymous: Boolean,
  override val bodyText: Option[String],
  override val deprecatedName: Option[String],
  override val implicitClassNames: Array[String],
) extends ScNamedStubBase[ScParameter](parent, elementType, name) with ScParameterStub
