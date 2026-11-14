package org.jetbrains.plugins.scala.actions

import consulo.annotation.component.ExtensionImpl
import consulo.fileTemplate.{FileTemplateContributor, FileTemplateRegistrator}

/**
 * upstream: `internalFileTemplate` registrations in plugin.xml
 */
@ExtensionImpl
final class ScalaFileTemplateContributor extends FileTemplateContributor {
  override def register(registrator: FileTemplateRegistrator): Unit =
    Seq("Scala Class", "Scala CaseClass", "Scala CaseObject", "Scala Trait", "Scala Object", "Package Object", "Scala File", "Scala Enum", "Scala Worksheet")
      .foreach(registrator.registerInternalTemplate)
}
