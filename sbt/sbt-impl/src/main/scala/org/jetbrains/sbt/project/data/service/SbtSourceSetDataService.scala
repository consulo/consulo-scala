package org.jetbrains.sbt.project.data.service

import consulo.annotation.component.ExtensionImpl

import consulo.externalSystem.model.Key
import consulo.module.Module
import org.jetbrains.plugins.scala.shared.util.SbtModuleType.sbtSourceSetModuleType
import org.jetbrains.sbt.project.module.SbtSourceSetData

@ExtensionImpl
class SbtSourceSetDataService extends AbstractSbtModuleDataService[SbtSourceSetData] {

  override def getTargetDataKey: Key[SbtSourceSetData] = SbtSourceSetData.Key

  override protected def moduleType: String =
    sbtSourceSetModuleType

  override protected def generateNewName(
    parentModule: Module,
    data: SbtSourceSetData,
    parentModuleActualName: String
  ): Option[String] =
    Some(s"$parentModuleActualName.${data.getModuleName}")
}
