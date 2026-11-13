package org.jetbrains.plugins.scala.lang.psi.stubs.index

import consulo.language.psi.stub.StubIndexKey
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScExtension

class ScTopLevelExtensionByPackageIndex extends ScStringStubIndexExtension[ScExtension] {
  override def getKey: StubIndexKey[String, ScExtension] =
    ScalaIndexKeys.TOP_LEVEL_EXTENSION_BY_PKG_KEY
}



