package org.jetbrains.plugins.scala.runner

import consulo.annotation.component.{ComponentScope, ExtensionAPI}
import consulo.application.Application
import consulo.execution.action.RunConfigurationProducer
import consulo.project.Project

/**
 * Disables the Scala run configuration producers (applications and tests) in projects where another build tool
 * provides them, for example Bazel.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
trait ScalaRunConfigurationProducerSuppressor {
  def shouldSuppress(producer: RunConfigurationProducer[?], project: Project): Boolean
}

object ScalaRunConfigurationProducerSuppressor {
  def isSuppressed(producer: RunConfigurationProducer[?], project: Project): Boolean =
    project != null &&
      Application.get().getExtensionPoint(classOf[ScalaRunConfigurationProducerSuppressor])
        .anyMatchSafe(_.shouldSuppress(producer, project))
}
