package org.jetbrains.plugins.scala.lang.psi.stubs.index

import consulo.annotation.component.ExtensionImpl

import com.intellij.java.language.psi.PsiClass
import consulo.language.psi.stub.{StringStubIndexExtension, StubIndexKey}

@ExtensionImpl
class ScShortClassNameIndex extends StringStubIndexExtension[PsiClass] {
  override def getKey: StubIndexKey[String, PsiClass] =
    ScalaIndexKeys.SHORT_NAME_KEY
}

@ExtensionImpl
class ScNotVisibleInJavaShortClassNameIndex extends StringStubIndexExtension[PsiClass] {
  override def getKey: StubIndexKey[String, PsiClass] =
    ScalaIndexKeys.NOT_VISIBLE_IN_JAVA_SHORT_NAME_KEY
}

@ExtensionImpl
class ScShortNamePackageObjectIndex extends StringStubIndexExtension[PsiClass] {
  override def getKey: StubIndexKey[String, PsiClass] =
    ScalaIndexKeys.PACKAGE_OBJECT_SHORT_NAME_KEY
}
