package org.jetbrains.sbt.settings

import consulo.annotation.component.ExtensionImpl
import consulo.configurable.search.{SearchableOptionContributor, SearchableOptionProcessor}
import consulo.localize.LocalizeValue
import org.jetbrains.sbt.SbtBundle
import org.jetbrains.sbt.project.SbtProjectSystem

// upstream: SbtExternalSystemConfigurable.createComponent links a default project settings control in TraverseUIMode,
// so that the options of the sbt project settings are indexed even without linked projects
@ExtensionImpl
final class SbtSearchableOptionContributor extends SearchableOptionContributor {

  override def processOptions(processor: SearchableOptionProcessor): Unit = {
    val configurableName = SbtProjectSystem.Id.getDisplayName
    SbtSearchableOptionContributor.OptionKeys.foreach { key =>
      val text = LocalizeValue.of(SbtBundle.message(key).replace("&", ""))
      processor.addOption(text, text, SbtSearchableOptionContributor.ConfigurableId, configurableName)
    }
  }
}

object SbtSearchableOptionContributor {

  private val ConfigurableId = "sbt.project.settings.configurable"

  private val OptionKeys = Seq(
    "sbt.settings.resolveClassifiers",
    "sbt.settings.resolveSbtClassifiers",
    "sbt.settings.useShellForImport",
    "sbt.settings.useShellForBuild",
    "sbt.settings.remoteDebug",
    "sbt.settings.scalaVersionPreference",
    "use.separate.compiler.output.paths",
    "generate.managed.sources.during.project.sync.label",
    "sbt.settings.useShell",
    "sbt.settings.download",
    "sbt.settings.shell.title",
    "sbt.settings.maxHeapSize",
    "sbt.settings.vmParams",
    "sbt.settings.sbtOptions",
    "sbt.settings.env.variables",
    "sbt.settings.sbtLauncher",
  )
}
