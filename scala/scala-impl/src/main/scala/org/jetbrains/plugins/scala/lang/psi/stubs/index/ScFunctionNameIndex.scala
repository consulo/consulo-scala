package org.jetbrains.plugins.scala.lang.psi.stubs.index

import consulo.language.psi.stub.StubIndexKey
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction

class ScFunctionNameIndex extends ScStringStubIndexExtension[ScFunction] {

  override def getKey: StubIndexKey[String, ScFunction] =
    ScalaIndexKeys.METHOD_NAME_KEY
}
