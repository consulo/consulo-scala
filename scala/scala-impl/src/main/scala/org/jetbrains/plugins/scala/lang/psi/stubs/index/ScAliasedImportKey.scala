package org.jetbrains.plugins.scala.lang.psi.stubs.index

import consulo.annotation.component.ExtensionImpl

import consulo.language.psi.stub.StubIndexKey
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports.ScImportSelector

@ExtensionImpl
class ScAliasedImportKey extends ScStringStubIndexExtension[ScImportSelector] {
  override def getKey: StubIndexKey[String, ScImportSelector] =
    ScalaIndexKeys.ALIASED_IMPORT_KEY
}
