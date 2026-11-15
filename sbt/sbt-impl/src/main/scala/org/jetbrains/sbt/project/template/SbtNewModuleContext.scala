package org.jetbrains.sbt.project.template

import consulo.module.creation.NewModuleWizardContextBase
import org.jetbrains.plugins.scala.ScalaVersion
import org.jetbrains.sbt.api.SbtVersion

class SbtNewModuleContext(isNewProject: Boolean) extends NewModuleWizardContextBase(isNewProject) {
  var sbtVersion: SbtVersion = SbtVersion.Latest.Sbt_1
  var scalaVersion: String = ScalaVersion.Latest.Scala_2_13.minor
  var downloadScalaSdkSources: Boolean = true
  var downloadSbtSources: Boolean = false
  var useIndentationSyntax: Boolean = false
  var packagePrefix: Option[String] = None
  var addSampleCode: Boolean = false
}
