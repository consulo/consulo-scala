package org.jetbrains.sbt.actions

import consulo.annotation.component.ExtensionImpl
import consulo.externalSystem.model.DataNode
import consulo.externalSystem.model.project.ModuleData
import consulo.localize.LocalizeValue
import consulo.module.Module
import org.jetbrains.sbt.{SbtBundle, SbtUtil}
import org.jetbrains.sbt.project.SbtProjectSystem

@ExtensionImpl
final class SbtDirectoryCompletionContributor
  extends ScalaDirectoryCompletionContributorBase(SbtProjectSystem.Id) {

  override def getDescription: LocalizeValue = LocalizeValue.of(SbtBundle.message("sbt.source.sets"))

  override protected def findModuleData(module: Module): Option[DataNode[? <: ModuleData]] =
    SbtUtil.getSbtModuleDataNode(module)
}
