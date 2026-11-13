package org.jetbrains.plugins.scala.lang.psi.stubs.index

import com.intellij.java.language.psi.PsiClass
import consulo.language.psi.stub.StubIndexKey

class ScAllClassNamesIndex extends ScStringStubIndexExtension[PsiClass] {
  override def getKey: StubIndexKey[String, PsiClass] =
    ScalaIndexKeys.ALL_CLASS_NAMES
}
