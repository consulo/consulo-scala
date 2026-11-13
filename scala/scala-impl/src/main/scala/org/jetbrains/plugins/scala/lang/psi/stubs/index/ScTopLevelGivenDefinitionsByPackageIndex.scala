package org.jetbrains.plugins.scala.lang.psi.stubs.index

import consulo.annotation.component.ExtensionImpl

import consulo.language.psi.stub.StubIndexKey
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScGivenDefinition

@ExtensionImpl
class ScTopLevelGivenDefinitionsByPackageIndex extends ScStringStubIndexExtension[ScGivenDefinition] {
  override def getKey: StubIndexKey[String, ScGivenDefinition] =
    ScalaIndexKeys.TOP_LEVEL_GIVEN_DEFINITIONS_BY_PKG_KEY
}
