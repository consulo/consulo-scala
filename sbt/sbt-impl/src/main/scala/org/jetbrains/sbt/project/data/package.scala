package org.jetbrains.sbt.project

import consulo.externalSystem.model.DataNode
import consulo.externalSystem.model.project.ModuleData
import consulo.module.{Module, ModuleManager}
import consulo.project.Project

import java.util
import scala.jdk.CollectionConverters._

package object data {
  private[data] def toJavaSet[A](set: Set[A]): java.util.Set[A] = new util.HashSet[A](set.asJava)
  private[data] def toJavaMap[K, V](map: Map[K, V]): java.util.Map[K, V] = new util.HashMap[K, V](map.asJava)

  // upstream: reads AbstractModuleDataService.MODULE_KEY user data of the parent node, Consulo data nodes have no user data
  private[data] def findModuleForParentOfDataNode[T](dataNode: DataNode[T])(implicit project: Project): Option[Module] = {
    val parentNode = dataNode.getParent
    if (parentNode != null) parentNode.getData match {
      case moduleData: ModuleData => Option(ModuleManager.getInstance(project).findModuleByName(moduleData.getInternalName))
      case _ => None
    }
    else None
  }
}
