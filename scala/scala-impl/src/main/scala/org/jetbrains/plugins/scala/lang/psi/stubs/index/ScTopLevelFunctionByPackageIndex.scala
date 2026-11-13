package org.jetbrains.plugins.scala.lang.psi.stubs.index

import consulo.annotation.component.ExtensionImpl

import consulo.language.psi.stub.StubIndexKey
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction

@ExtensionImpl
class ScTopLevelFunctionByPackageIndex extends ScStringStubIndexExtension[ScFunction] {
  override def getKey: StubIndexKey[String, ScFunction] =
    ScalaIndexKeys.TOP_LEVEL_FUNCTION_BY_PKG_KEY
}
