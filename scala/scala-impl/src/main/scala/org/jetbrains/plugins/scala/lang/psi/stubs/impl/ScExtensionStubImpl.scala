package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{StubBase, StubElement}
import consulo.language.ast.IElementType
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScExtension
import org.jetbrains.plugins.scala.lang.psi.stubs.ScExtensionStub

class ScExtensionStubImpl(
  parent:                            StubElement[? <: PsiElement],
  elementType:                       IElementType,
  override val isTopLevel:           Boolean,
  override val topLevelQualifier:    Option[String],
  override val extensionTargetClass: Option[String]
) extends StubBase[ScExtension](parent, elementType)
    with ScExtensionStub
