package org.jetbrains.plugins.scala.lang.psi.stubs.index

import consulo.annotation.component.ExtensionImpl

import consulo.language.psi.stub.StubIndexKey
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScPackaging

@ExtensionImpl
final class ScPackagingFqnIndex extends ScFqnHashStubIndexExtension[ScPackaging] {

  override def getKey: StubIndexKey[CharSequence, ScPackaging] =
    ScalaIndexKeys.PACKAGE_FQN_KEY
}

object ScPackagingFqnIndex {
  def instance: ScPackagingFqnIndex = new ScPackagingFqnIndex
}