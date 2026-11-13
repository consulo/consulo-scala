package org.jetbrains.plugins.scala.lang.psi.stubs.index

import consulo.annotation.component.ExtensionImpl

import consulo.language.psi.stub.StubIndexKey
import org.jetbrains.plugins.scala.lang.psi.api.base.ScAnnotation

@ExtensionImpl
class ScAnnotatedMemberIndex extends ScStringStubIndexExtension[ScAnnotation] {
  override def getKey: StubIndexKey[String, ScAnnotation] =
    ScalaIndexKeys.ANNOTATED_MEMBER_KEY
}
