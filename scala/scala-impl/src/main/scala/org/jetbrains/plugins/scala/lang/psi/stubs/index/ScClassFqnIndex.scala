package org.jetbrains.plugins.scala.lang.psi.stubs.index

import consulo.annotation.component.ExtensionImpl

import com.intellij.java.language.psi.PsiClass
import consulo.language.psi.stub.StubIndexKey

@ExtensionImpl
final class ScClassFqnIndex extends ScFqnHashStubIndexExtension[PsiClass] {
  override def getKey: StubIndexKey[CharSequence, PsiClass] =
    ScalaIndexKeys.CLASS_FQN_KEY
}

object ScClassFqnIndex {
  def instance: ScClassFqnIndex = new ScClassFqnIndex
}
