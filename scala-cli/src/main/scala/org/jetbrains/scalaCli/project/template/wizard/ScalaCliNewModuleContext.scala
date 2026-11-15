package org.jetbrains.scalaCli.project.template.wizard

import consulo.module.creation.NewModuleWizardContextBase
import org.jetbrains.plugins.scala.ScalaVersion

class ScalaCliNewModuleContext(isNewProject: Boolean) extends NewModuleWizardContextBase(isNewProject) {
  var scalaVersion: String = ScalaVersion.Latest.Scala_2_13.minor
  var useIndentationSyntax: Boolean = false
  var addSampleCode: Boolean = false
}
