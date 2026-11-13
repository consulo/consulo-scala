package org.jetbrains.plugins.scala.lang.psi.stubs.index

import consulo.annotation.component.ExtensionImpl

import consulo.language.psi.stub.StubIndexKey
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScTypeAlias

@ExtensionImpl
class ScTypeAliasNameIndex extends ScStringStubIndexExtension[ScTypeAlias] {

  override def getKey: StubIndexKey[String, ScTypeAlias] =
    ScalaIndexKeys.TYPE_ALIAS_NAME_KEY
}

@ExtensionImpl
class ScStableTypeAliasNameIndex extends ScStringStubIndexExtension[ScTypeAlias] {
  override def getKey: StubIndexKey[String, ScTypeAlias] =
    ScalaIndexKeys.STABLE_ALIAS_NAME_KEY
}

@ExtensionImpl
class ScStableTypeAliasFqnIndex extends ScFqnHashStubIndexExtension[ScTypeAlias] {
  override def getKey: StubIndexKey[CharSequence, ScTypeAlias] =
    ScalaIndexKeys.STABLE_ALIAS_FQN_KEY
}
object ScStableTypeAliasFqnIndex {
  def instance: ScStableTypeAliasFqnIndex = new ScStableTypeAliasFqnIndex
}

@ExtensionImpl
class ScAliasedClassNameKey extends ScStringStubIndexExtension[ScTypeAlias] {
  override def getKey: StubIndexKey[String, ScTypeAlias] =
    ScalaIndexKeys.ALIASED_CLASS_NAME_KEY
}
