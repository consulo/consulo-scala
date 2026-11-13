package org.jetbrains.plugins.scala.lang.psi.stubs.index

import consulo.annotation.component.ExtensionImpl

import consulo.language.psi.stub.StubIndexKey
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports.ScExportStmt

@ExtensionImpl
class ScTopLevelExportByPackageIndex extends ScStringStubIndexExtension[ScExportStmt] {
  override def getKey: StubIndexKey[String, ScExportStmt] = ScalaIndexKeys.TOP_LEVEL_EXPORT_BY_PKG_KEY
}
