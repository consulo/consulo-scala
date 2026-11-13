package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.stub.IStubElementType
import consulo.language.psi.PsiElement
import consulo.language.psi.stub.StubElement
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScTypeAlias
import org.jetbrains.plugins.scala.lang.psi.stubs.ScTypeAliasStub

class ScTypeAliasStubImpl(
  parent:                          StubElement[? <: PsiElement],
  elementType:                     IStubElementType[?, ?],
  name:                            String,
  override val typeText:           Option[String],
  override val lowerBoundText:     Option[String],
  override val upperBoundText:     Option[String],
  override val contextBoundsTexts: Array[String],
  override val isLocal:            Boolean,
  override val isDeclaration:      Boolean,
  override val isStableQualifier:  Boolean,
  override val stableQualifier:    Option[String],
  override val isTopLevel:         Boolean,
  override val topLevelQualifier:  Option[String],
  override val classType:          Option[String]
) extends ScNamedStubBase[ScTypeAlias](parent, elementType, name)
    with ScTypeAliasStub {
  override def viewBoundsTexts: Array[String] = Array.empty
}
