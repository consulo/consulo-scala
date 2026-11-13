package org.jetbrains.plugins.scala.lang.psi.stubs.index

import consulo.annotation.component.ExtensionImpl
import consulo.language.psi.stub.StubIndexKey
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScValueOrVariable

@ExtensionImpl
class ScTopLevelPropertyByPackageIndex extends ScStringStubIndexExtension[ScValueOrVariable] {
  override def getKey: StubIndexKey[String, ScValueOrVariable] =
    ScalaIndexKeys.TOP_LEVEL_VAL_OR_VAR_BY_PKG_KEY
}
