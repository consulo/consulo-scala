package org.jetbrains.plugins.scala.project.template

import consulo.content.bundle.Sdk
import consulo.module.creation.NewModuleWizardContextBase
import org.jetbrains.plugins.scala.project.ScalaLibraryProperties

final class ScalaNewModuleWizardContext(isNewProject: Boolean) extends NewModuleWizardContextBase(isNewProject) {
  var sdk: Option[Sdk] = None

  var scalaSdk: Option[ScalaSdkDescriptor] = None

  /** resolved when the Scala SDK is selected (compiler bridge and REPL classpath may be downloaded) */
  var scalaSdkProperties: Option[ScalaLibraryProperties] = None
}
