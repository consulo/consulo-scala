package org.jetbrains.plugins.scala.structureView

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import org.jetbrains.plugins.scala.api.Scala3Language

// upstream: one extension; Consulo's StructureAwareNavBarModelExtension compares element languages by identity
@ExtensionImpl(order = "after ScalaNavBarModelExtension, before JavaNavBarExtension, before ScratchNavBarExtension")
final class Scala3NavBarModelExtension extends ScalaNavBarModelExtension {
  override def getLanguage: Language = Scala3Language.INSTANCE
}
