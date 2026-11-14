package org.jetbrains.sbt.project

import consulo.externalSystem.model.{Key, ProjectKeys}
import consulo.externalSystem.service.project.AbstractExternalEntityData
import org.jetbrains.sbt.api.RichSeq

import java.util.List as JList

/**
 * Data storing external modules ids that own the given shared sources module.
 * This data is only put in shared sources modules.
 *
 * @param ownerModuleIds id of the modules that own shared sources module
 */
// upstream: lives in sbt-api
case class SharedSourcesOwnersData(
  ownerModuleIds: JList[String],
) extends AbstractExternalEntityData(SbtProjectSystem.Id) with Product

object SharedSourcesOwnersData {
  val Key: Key[SharedSourcesOwnersData] = new Key(classOf[SharedSourcesOwnersData].getName,  ProjectKeys.MODULE.getProcessingWeight + 3)

  def apply(owners: Seq[String]): SharedSourcesOwnersData =
    SharedSourcesOwnersData(owners.toJavaList)
}
