package org.jetbrains.plugins.scala.lang.psi.stubs.index

import consulo.annotation.component.ExtensionImpl

import consulo.language.psi.stub.StubIndexKey
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject

@ExtensionImpl
class ScImplicitObjectKey extends ScStringStubIndexExtension[ScObject] {

  override def getKey: StubIndexKey[String, ScObject] =
    ScalaIndexKeys.IMPLICIT_OBJECT_KEY
}
