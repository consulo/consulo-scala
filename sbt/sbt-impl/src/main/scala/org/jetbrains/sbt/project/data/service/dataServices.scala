package org.jetbrains.sbt.project.data.service

import consulo.annotation.component.ExtensionImpl
import consulo.externalSystem.model.{DataNode, Key}
import consulo.module.Module
import consulo.project.Project
import org.jetbrains.plugins.scala.project.external.ScalaAbstractProjectDataService
import org.jetbrains.sbt.project.data.{SbtCommandData, SbtModuleData, SbtSettingData, SbtTaskData}

import java.util

@ExtensionImpl
class SbtTaskDataService extends DefaultDataService[SbtTaskData, Module](SbtTaskData.Key)

@ExtensionImpl
class SbtSettingDataService extends DefaultDataService[SbtSettingData, Module](SbtSettingData.Key)

@ExtensionImpl
class SbtCommandDataService extends DefaultDataService[SbtCommandData, Module](SbtCommandData.Key)

// upstream: SbtModuleDataWorkspaceDataService stores SbtModuleData in the IntelliJ workspace model, not available in Consulo
@ExtensionImpl
class SbtModuleDataService extends DefaultDataService[SbtModuleData, Module](SbtModuleData.Key)


/**
 * @note data services for external system entities are required for proper serialization (even if they do not contain any logic)
 */
abstract class DefaultDataService[E,I](key: Key[E]) extends ScalaAbstractProjectDataService[E,I](key) {
  override def importData(
    toImport: util.Collection[DataNode[E]],
    project: Project,
    synchronous: Boolean
  ): Unit = ()

  override def removeData(toRemove: util.Collection[? <: I], project: Project, synchronous: Boolean): Unit = ()
}
