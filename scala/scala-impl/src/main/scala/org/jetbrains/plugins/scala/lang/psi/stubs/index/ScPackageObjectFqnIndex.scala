package org.jetbrains.plugins.scala.lang.psi.stubs.index

import consulo.annotation.component.ExtensionImpl

import com.intellij.java.language.psi.PsiClass
import consulo.language.psi.stub.StubIndexKey

@ExtensionImpl
final class ScPackageObjectFqnIndex extends ScFqnHashStubIndexExtension[PsiClass] {

  override def getKey: StubIndexKey[CharSequence, PsiClass] =
    ScalaIndexKeys.PACKAGE_OBJECT_FQN_KEY
}

object ScPackageObjectFqnIndex {
  def instance: ScPackageObjectFqnIndex = new ScPackageObjectFqnIndex
}
