package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{StubBase, StubElement}
import consulo.language.ast.IElementType
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScValueOrVariable
import org.jetbrains.plugins.scala.lang.psi.stubs.ScPropertyStub

final class ScPropertyStubImpl[P <: ScValueOrVariable](
  parent:                         StubElement[? <: PsiElement],
  elementType:                    IElementType,
  override val isDeclaration:     Boolean,
  override val isImplicit:        Boolean,
  override val names:             Array[String],
  override val typeText:          Option[String],
  override val bodyText:          Option[String],
  override val isLocal:           Boolean,
  override val classNames:        Array[String],
  override val isTopLevel:        Boolean,
  override val topLevelQualifier: Option[String]
) extends StubBase[P](parent, elementType) with ScPropertyStub[P]
