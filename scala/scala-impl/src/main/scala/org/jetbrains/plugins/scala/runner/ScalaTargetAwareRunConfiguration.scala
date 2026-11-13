package org.jetbrains.plugins.scala.runner

import consulo.content.bundle.{Sdk, SdkTable}
import consulo.platform.Platform
import consulo.execution.CantRunException
import consulo.execution.configuration.{ConfigurationFactory, ModuleBasedConfiguration, RunConfigurationModule}
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.target.java.{JavaLanguageRuntimeConfiguration, JavaLanguageRuntimeType}
import com.intellij.execution.target.{LanguageRuntimeType, TargetEnvironmentAwareRunProfile, TargetEnvironmentConfiguration}

import java.nio.file.Path

/**
 * Base class for Scala run configurations that support remote execution targets (eel/WSL).
 *
 * Implements the [[TargetEnvironmentAwareRunProfile]] interface following the same pattern
 * as `com.intellij.execution.junit.JUnitConfiguration`.
 */
// upstream: @ApiStatus.Internal
abstract class ScalaTargetAwareRunConfiguration[M <: RunConfigurationModule, S](
  name: String,
  module: M,
  factory: ConfigurationFactory
) extends ModuleBasedConfiguration[M, S](name, module, factory)
  with TargetEnvironmentAwareRunProfile {

  override def clone(): ModuleBasedConfiguration[?, ?] = super.clone()

  /**
   * An alternative JRE path to check when determining whether the run configuration
   * runs under a remote JDK. Subclasses that support alternative JRE settings
   * should override this to return the configured alternative JRE path.
   */
  protected def alternativeJrePath: Option[String] = None

  override def canRunOn(target: TargetEnvironmentConfiguration): Boolean =
    target.getRuntimes.findByType(classOf[JavaLanguageRuntimeConfiguration]) != null

  override def getDefaultLanguageRuntimeType: LanguageRuntimeType[?] =
    LanguageRuntimeType.EXTENSION_NAME.findExtension(classOf[JavaLanguageRuntimeType])

  override def getDefaultTargetName: String = getOptions.getRemoteTarget

  override def setDefaultTargetName(targetName: String): Unit = {
    getOptions.setRemoteTarget(targetName)
  }

  override def needPrepareTarget(): Boolean =
    super.needPrepareTarget() || runsUnderRemoteJdk()

  /**
   * Same as `com.intellij.execution.JavaRunConfigurationBase#runsUnderRemoteJdk`.
   */
  private def runsUnderRemoteJdk(): Boolean = {
    // upstream: !EelPathUtils.isPathLocal(path); in Consulo a remote JDK belongs to a remote platform
    jdkSatisfies(sdk => sdk.getPlatform != Platform.current())
  }

  /**
   * Same as `com.intellij.execution.JavaRunConfigurationBase#jdkHomeSatisfies`, but checks the SDK itself
   * (its platform), a JRE path without registered SDK is local.
   */
  private def jdkSatisfies(predicate: Sdk => Boolean): Boolean = {
    alternativeJrePath match {
      case Some(path) =>
        val sdk = SdkTable.getInstance().findSdk(path)
        return sdk != null && predicate(sdk)
      case None =>
    }

    val module = getConfigurationModule.getModule
    if (module != null) {
      val sdk = try {
        JavaParameters.getValidJdkToRunModule(module, /* productionOnly = */ false)
      } catch {
        case _: CantRunException => return false
      }
      return sdk != null && predicate(sdk)
    }

    false
  }
}
