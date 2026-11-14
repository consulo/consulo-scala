package org.jetbrains.sbt
package project
package data
package service

import consulo.annotation.component.ExtensionImpl
import consulo.externalSystem.model.DataNode
import consulo.externalSystem.util.{ExternalSystemConstants, Order}
import consulo.module.Module
import consulo.module.content.layer.orderEntry.{LibraryOrderEntry, ModuleOrderEntry, OrderEntry}
import consulo.project.Project
import org.jetbrains.plugins.scala.project.external.ScalaAbstractProjectDataService

import java.util
import scala.jdk.CollectionConverters.*

// upstream: AbstractModuleDataService.rearrangeOrderEntries (order of AbstractDependencyData) and
// ModuleDependencyData.isProductionOnTestDependency - Consulo's dependency data services do neither, so this service runs
// after them: entries without sbt data (module sources, JDK) stay first, the dependencies follow sorted by their order
@ExtensionImpl
@Order(ExternalSystemConstants.UNORDERED)
final class SbtDependencyOrderDataService extends ScalaAbstractProjectDataService[SbtDependencyOrderData, Module](SbtDependencyOrderData.Key) {

  override def importData(
    toImport: util.Collection[DataNode[SbtDependencyOrderData]],
    project: Project,
    synchronous: Boolean
  ): Unit = SbtModuleExtensions.executeProjectChangeAction(project, synchronous) {
    for {
      dataNode <- toImport.asScala
      module <- project.getIdeModuleByNode(dataNode)
    } {
      val SbtDependencyOrderData(orders, productionOnTest) = dataNode.getData
      SbtModuleExtensions.modifyModel(module) { model =>
        val entries = model.getOrderEntries.toSeq
        val keyed = entries.map(entry => entry -> SbtDependencyOrderDataService.orderKey(entry))

        keyed.foreach {
          case (entry: ModuleOrderEntry, Some(key)) if productionOnTest.contains(key) =>
            entry.setProductionOnTestDependency(true)
          case _ =>
        }

        val (withoutOrder, withOrder) = keyed.partition { case (_, key) => key.forall(k => !orders.containsKey(k)) }
        val sorted = withoutOrder.map(_._1) ++ withOrder.sortBy { case (_, key) => orders.get(key.get).intValue }.map(_._1)
        if (sorted != entries)
          model.rearrangeOrderEntries(sorted.toArray)
      }
    }
  }

  override def removeData(toRemove: util.Collection[? <: Module], project: Project, synchronous: Boolean): Unit = ()
}

private object SbtDependencyOrderDataService {

  private def orderKey(entry: OrderEntry): Option[String] = entry match {
    case module: ModuleOrderEntry => Some(SbtDependencyOrderData.moduleKey(module.getModuleName, module.getScope))
    case library: LibraryOrderEntry if library.getLibraryName != null => Some(SbtDependencyOrderData.libraryKey(library.getLibraryName, library.getScope))
    case _ => None
  }
}
