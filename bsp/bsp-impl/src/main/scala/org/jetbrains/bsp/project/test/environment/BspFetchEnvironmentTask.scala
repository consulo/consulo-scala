package org.jetbrains.bsp.project.test.environment

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import consulo.execution.BeforeRunTask
import consulo.util.dataholder.Key
import consulo.util.xml.serializer.annotation.{Attribute, Transient}
import org.jetbrains.bsp.project.test.environment.BspJvmEnvironment.BspTargetIdHolder

object BspFetchEnvironmentTask {
  // upstream: the Key identifying the before run task, Consulo identifies it by its class (see BspFetchEnvironmentTaskProvider)
  val runTaskId: String = "BSP.BeforeRunTask"

  val jvmEnvironmentKey: Key[JvmEnvironment] = Key.create("BSP.JvmTestEnvironment")
}

// upstream: writeExternal/readExternal of the CHOSEN_TARGET attribute - Consulo before run tasks are serialized as beans
class BspFetchEnvironmentTask
  extends BeforeRunTask
  with BspTargetIdHolder {

  private var chosenTarget: String = scala.compiletime.uninitialized

  @Attribute("CHOSEN_TARGET")
  def getChosenTarget: String = chosenTarget

  def setChosenTarget(value: String): Unit = chosenTarget = value

  @Transient
  def selected: Option[BuildTargetIdentifier] = Option(chosenTarget).map(new BuildTargetIdentifier(_))

  override def currentValue: Option[BuildTargetIdentifier] = selected

  override def update(value: BuildTargetIdentifier): Unit = chosenTarget = value.getUri
}
