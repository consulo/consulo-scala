package org.jetbrains.plugins.scala.lang.psi.stubs.index

import consulo.language.psi.stub.StubIndexKey
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScPackaging

final class ScPackagingFqnIndex extends ScFqnHashStubIndexExtension[ScPackaging] {

  override def getKey: StubIndexKey[CharSequence, ScPackaging] =
    ScalaIndexKeys.PACKAGE_FQN_KEY
}

object ScPackagingFqnIndex {
  def instance: ScPackagingFqnIndex = new ScPackagingFqnIndex
}