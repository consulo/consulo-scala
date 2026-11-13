package org.jetbrains.plugins.scala.lang.psi.stubs.index

import consulo.annotation.component.ExtensionImpl

import com.intellij.java.language.psi.PsiClass
import consulo.language.psi.stub.StubIndexKey

@ExtensionImpl
class ScAllClassNamesIndex extends ScStringStubIndexExtension[PsiClass] {
  override def getKey: StubIndexKey[String, PsiClass] =
    ScalaIndexKeys.ALL_CLASS_NAMES
}
