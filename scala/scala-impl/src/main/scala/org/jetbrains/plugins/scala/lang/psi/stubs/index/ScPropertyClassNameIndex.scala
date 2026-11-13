package org.jetbrains.plugins.scala.lang.psi.stubs.index

import consulo.language.psi.stub.StubIndexKey
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScValueOrVariable

class ScPropertyClassNameIndex extends ScStringStubIndexExtension[ScValueOrVariable] {

  override def getKey: StubIndexKey[String, ScValueOrVariable] = ScalaIndexKeys.PROPERTY_CLASS_NAME_KEY
}
