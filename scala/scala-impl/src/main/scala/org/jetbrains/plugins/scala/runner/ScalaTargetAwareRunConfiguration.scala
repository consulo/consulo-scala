package org.jetbrains.plugins.scala.runner

import consulo.execution.configuration.{ConfigurationFactory, ModuleBasedConfiguration, RunConfigurationModule}

/**
 * Base class for Scala run configurations.
 *
 * upstream: implements TargetEnvironmentAwareRunProfile (remote execution targets, eel/WSL),
 * Consulo has no execution targets, remote JDKs are handled by the platform of the SDK.
 *
 * @tparam S upstream: the state type of IntelliJ `ModuleBasedConfiguration[M, S]`, unused
 */
// upstream: @ApiStatus.Internal
abstract class ScalaTargetAwareRunConfiguration[M <: RunConfigurationModule, S](
  name: String,
  module: M,
  factory: ConfigurationFactory
) extends ModuleBasedConfiguration[M](name, module, factory) {

  override def clone(): ModuleBasedConfiguration[?] = super.clone()

  /**
   * An alternative JRE path to check when determining whether the run configuration
   * runs under a remote JDK. Subclasses that support alternative JRE settings
   * should override this to return the configured alternative JRE path.
   */
  protected def alternativeJrePath: Option[String] = None
}
