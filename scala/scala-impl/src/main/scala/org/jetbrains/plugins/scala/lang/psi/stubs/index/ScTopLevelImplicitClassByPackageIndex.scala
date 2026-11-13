package org.jetbrains.plugins.scala.lang.psi.stubs.index

import consulo.annotation.component.ExtensionImpl

import consulo.language.psi.stub.StubIndexKey
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScClass

@ExtensionImpl
class ScTopLevelImplicitClassByPackageIndex extends ScStringStubIndexExtension[ScClass] {
  override def getKey: StubIndexKey[String, ScClass] =
    ScalaIndexKeys.TOP_LEVEL_IMPLICIT_CLASS_BY_PKG_KEY
}
