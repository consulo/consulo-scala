package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.stub.IStubElementType
import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{StubBase, StubElement}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports.{ScExportStmt, ScImportOrExportStmt, ScImportStmt}
import org.jetbrains.plugins.scala.lang.psi.stubs.{ScExportStmtStub, ScImportOrExportStmtStub, ScImportStmtStub}

abstract sealed class ScImportOrExportStmtStubImpl[T <: ScImportOrExportStmt](
  parent: StubElement[? <: PsiElement],
  elementType: IStubElementType[?, ?],
  override val importText: String
) extends StubBase[T](parent, elementType)
  with ScImportOrExportStmtStub[T]

class ScImportStmtStubImpl(
  parent: StubElement[? <: PsiElement],
  elementType: IStubElementType[?, ?],
  importText: String
) extends ScImportOrExportStmtStubImpl[ScImportStmt](parent, elementType, importText) with ScImportStmtStub

class ScExportStmtStubImpl(
  parent: StubElement[? <: PsiElement],
  elementType: IStubElementType[?, ?],
  importText: String,
  override val isTopLevel: Boolean,
  override val topLevelQualifier: Option[String]
) extends ScImportOrExportStmtStubImpl[ScExportStmt](parent, elementType, importText)
  with ScExportStmtStub
