package org.jetbrains.plugins.scala.lang.psi.stubs.index

import consulo.annotation.component.ExtensionImpl

import consulo.language.psi.stub.StubIndexKey
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScClassParameter

@ExtensionImpl
final class ScClassParameterNameIndex extends ScStringStubIndexExtension[ScClassParameter] {

  override def getKey: StubIndexKey[String, ScClassParameter] = ScalaIndexKeys.CLASS_PARAMETER_NAME_KEY
}
