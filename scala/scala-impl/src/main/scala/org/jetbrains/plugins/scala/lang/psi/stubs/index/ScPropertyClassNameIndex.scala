package org.jetbrains.plugins.scala.lang.psi.stubs.index

import consulo.annotation.component.ExtensionImpl

import consulo.language.psi.stub.StubIndexKey
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScValueOrVariable

@ExtensionImpl
class ScPropertyClassNameIndex extends ScStringStubIndexExtension[ScValueOrVariable] {

  override def getKey: StubIndexKey[String, ScValueOrVariable] = ScalaIndexKeys.PROPERTY_CLASS_NAME_KEY
}
