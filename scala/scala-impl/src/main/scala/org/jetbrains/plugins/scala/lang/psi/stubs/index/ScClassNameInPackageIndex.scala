package org.jetbrains.plugins.scala.lang.psi.stubs.index

import consulo.annotation.component.ExtensionImpl

import com.intellij.java.language.psi.PsiClass
import consulo.language.psi.stub.StubIndexKey

@ExtensionImpl
class ScClassNameInPackageIndex extends ScStringStubIndexExtension[PsiClass] {
  override def getKey: StubIndexKey[String, PsiClass] =
    ScalaIndexKeys.CLASS_NAME_IN_PACKAGE_KEY
}
