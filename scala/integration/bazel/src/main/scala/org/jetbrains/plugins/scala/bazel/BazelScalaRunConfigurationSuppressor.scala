package org.jetbrains.plugins.scala.bazel

import consulo.annotation.component.ExtensionImpl
import consulo.execution.action.RunConfigurationProducer
import consulo.project.Project
import org.jetbrains.plugins.scala.runner.{BaseScalaApplicationConfigurationProducer, ScalaRunConfigurationProducerSuppressor}
import org.jetbrains.plugins.scala.testingSupport.test.AbstractTestConfigurationProducer

/**
 * Disables non-Bazel run config producers for Bazel projects: applications and tests are run through Bazel
 * (see [[BazelScalaRunConfigurationProducer]]).
 */
@ExtensionImpl
final class BazelScalaRunConfigurationSuppressor extends ScalaRunConfigurationProducerSuppressor {
  override def shouldSuppress(runConfigurationProducer: RunConfigurationProducer[?], project: Project): Boolean = {
    if (!org.jetbrains.bazel.config.project.isBazelProject(project)) return false
    runConfigurationProducer match {
      case _: AbstractTestConfigurationProducer[?] => true
      case _: BaseScalaApplicationConfigurationProducer[?] => true
      case _ => false
    }
  }
}
