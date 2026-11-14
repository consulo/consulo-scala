package org.jetbrains.sbt
package project.data

import consulo.externalSystem.model.project.{ContentRootData, LibraryData, LibraryDependencyData, LibraryLevel, ModuleData, ModuleDependencyData}
import consulo.externalSystem.rt.model.ExternalSystemSourceType
import consulo.externalSystem.service.project.ProjectData
import consulo.externalSystem.model.{DataNode, Key, ProjectKeys}
import org.jetbrains.plugins.scala.extensions.PathExt
import org.jetbrains.sbt.project.module.{SbtNestedModuleData, SbtSourceSetData}
import org.jetbrains.sbt.project.{SbtProjectSystem, SharedSourcesOwnersData, SourceSetType}

import java.net.URI
import java.nio.file.Path
import scala.jdk.CollectionConverters.*

class ProjectNode(override val data: ProjectData)
  extends Node[ProjectData] {
  def this(name: String, ideProjectFileDirectoryPath: String, linkedExternalProjectPath: String) = {
    this(new ProjectData(SbtProjectSystem.Id, name, ideProjectFileDirectoryPath, linkedExternalProjectPath))
  }

  override protected def key: Key[ProjectData] = ProjectKeys.PROJECT
}

class ModuleNode(override val data: ModuleData)
  extends Node[ModuleData] {
  // upstream: typeId is the IntelliJ module type id, Consulo has no module types
  def this(typeId: String, projectId: String, name: String, moduleFileDirectoryPath: String, externalConfigPath: String) = {
    this(new ModuleData(projectId, SbtProjectSystem.Id, name, moduleFileDirectoryPath, externalConfigPath))
  }

  override protected def key: Key[ModuleData] = ProjectKeys.MODULE
}

object ModuleNode {
  /**
    * Generate a formatted ID with project id and URI.
    * This prevent ID conflicts on multi module projects where different modules have same value as ID
    * @param projectId project ID
    * @param projectURI project root path or repository url
    * @return
    */
  def combinedId(projectId: String, projectURI: Option[URI]): String =
    projectURI match {
      case Some(uri) => f"$projectId [$uri]"
      case None => projectId
    }
}

class NestedModuleNode(override val data: SbtNestedModuleData)
  extends Node[SbtNestedModuleData] {

  def this(typeId: String, projectId: String, externalName: String, moduleFileDirectoryPath: String, externalConfigPath: String) = {
    this(SbtNestedModuleData(projectId, externalName, moduleFileDirectoryPath, externalConfigPath, typeId))
  }

  // upstream: SbtNestedModuleData.Key
  override protected def key: Key[SbtNestedModuleData] = ProjectKeys.MODULE.asInstanceOf[Key[SbtNestedModuleData]]
}

class SbtSourceSetModuleNode(override val data: SbtSourceSetData, val sourceSetType: SourceSetType)
  extends Node[SbtSourceSetData] {

  def this(typeId: String, projectId: String, sourceSetType: SourceSetType, moduleFileDirectoryPath: String, externalConfigPath: String) = {
    this(SbtSourceSetData(projectId, sourceSetType.toString, moduleFileDirectoryPath, externalConfigPath, typeId), sourceSetType)
  }

  // upstream: SbtSourceSetData.Key
  override protected def key: Key[SbtSourceSetData] = ProjectKeys.MODULE.asInstanceOf[Key[SbtSourceSetData]]
}

class LibraryNode(override val data: LibraryData)
  extends Node[LibraryData] {
  def this(name: String, resolved: Boolean) = {
    this(new LibraryData(SbtProjectSystem.Id, name, !resolved))
  }

  def addPaths(pathType: String, paths: Seq[String]): Unit = {
    paths.foreach(data.addPath(pathType, _))
  }

  override protected def key: Key[LibraryData] = ProjectKeys.LIBRARY
}

class SharedSourcesOwnersNode(override val data: SharedSourcesOwnersData)
  extends Node[SharedSourcesOwnersData] {

  override protected def key: Key[SharedSourcesOwnersData] = SharedSourcesOwnersData.Key
}

class ContentRootNode(override val data: ContentRootData)
  extends Node[ContentRootData] {

  def this(path: Path) = {
    this(new ContentRootData(SbtProjectSystem.Id, path.toCanonicalPath.toString))
  }

  def storeNioPaths(sourceType: ExternalSystemSourceType, paths: Seq[Path]): Unit = {
    paths.foreach(p => data.storePath(sourceType, p.toCanonicalPath.toString))
  }

  def storeGeneratedResourcePaths(paths: Seq[Path]): Unit =
    if (paths.nonEmpty) {
      add(new GeneratedResourcesNode(SbtGeneratedResourcesData(new java.util.ArrayList(paths.map(_.toCanonicalPath.toString).asJava))))
    }

  override protected def key: Key[ContentRootData] = ProjectKeys.CONTENT_ROOT
}

// upstream: AbstractDependencyData.setOrder / ModuleDependencyData.setProductionOnTestDependency - not in Consulo's
// dependency data; kept on the nodes and passed to SbtDependencyOrderDataService as SbtDependencyOrderData
sealed trait OrderedDependencyNode {
  private var order: Int = 0

  def setOrder(order: Int): Unit =
    this.order = order

  def getOrder: Int = order

  def orderKey: String
}

class ModuleDependencyNode(override val data: ModuleDependencyData)
  extends Node[ModuleDependencyData] with OrderedDependencyNode {
  def this(ownerModule: ModuleData, module: ModuleData) = {
    this(new ModuleDependencyData(ownerModule, module))
  }

  private var productionOnTest: Boolean = false

  def setProductionOnTestDependency(value: Boolean): Unit =
    productionOnTest = value

  def isProductionOnTestDependency: Boolean = productionOnTest

  override def orderKey: String = SbtDependencyOrderData.moduleKey(data.getTarget.getInternalName, data.getScope)

  override protected def key: Key[ModuleDependencyData] = ProjectKeys.MODULE_DEPENDENCY
}

class LibraryDependencyNode(override val data: LibraryDependencyData)
  extends Node[LibraryDependencyData] with OrderedDependencyNode {
  def this(ownerModule: ModuleData, library: LibraryData, level: LibraryLevel) = {
    this(new LibraryDependencyData(ownerModule, library, level))
  }

  override def orderKey: String = SbtDependencyOrderData.libraryKey(data.getTarget.getInternalName, data.getScope)

  override protected def key: Key[LibraryDependencyData] = ProjectKeys.LIBRARY_DEPENDENCY
}

class DependencyOrderNode(override val data: SbtDependencyOrderData) extends Node[SbtDependencyOrderData] {
  override protected def key: Key[SbtDependencyOrderData] = SbtDependencyOrderData.Key
}

class SbtProjectNode(override val data: SbtProjectData) extends Node[SbtProjectData] {
  override protected def key: Key[SbtProjectData] = SbtProjectData.Key
}

class SbtModuleNode(override val data: SbtModuleData) extends Node[SbtModuleData] {
  override protected def key: Key[SbtModuleData] = SbtModuleData.Key
}

class SbtSettingNode(override val data: SbtSettingData) extends Node[SbtSettingData] {
  override protected def key: Key[SbtSettingData] = SbtSettingData.Key
}

class SbtDisplayModuleNameNode(override val data: DisplayModuleNameData) extends Node[DisplayModuleNameData] {
  override protected def key: Key[DisplayModuleNameData] = DisplayModuleNameData.Key

  def this(name: String) = this(DisplayModuleNameData(name))
}

class SbtTaskNode(override val data: SbtTaskData) extends Node[SbtTaskData] {
  override protected def key: Key[SbtTaskData] = SbtTaskData.Key
}

class SbtCommandNode(override val data: SbtCommandData) extends Node[SbtCommandData] {
  override protected def key: Key[SbtCommandData] = SbtCommandData.Key
}

class ScalaSdkNode(override val data: SbtScalaSdkData) extends Node[SbtScalaSdkData] {
  override protected def key: Key[SbtScalaSdkData] = SbtScalaSdkData.Key
}

class GeneratedResourcesNode(override val data: SbtGeneratedResourcesData) extends Node[SbtGeneratedResourcesData] {
  override protected def key: Key[SbtGeneratedResourcesData] = SbtGeneratedResourcesData.Key
}

class ModuleExtNode(override val data: SbtModuleExtData) extends Node[SbtModuleExtData] {
  override protected def key: Key[SbtModuleExtData] = SbtModuleExtData.Key
}

class Play2ProjectNode(override val data: SbtPlay2ProjectData) extends Node[SbtPlay2ProjectData] {
  override def key: Key[SbtPlay2ProjectData] = SbtPlay2ProjectData.Key
}

class SbtBuildModuleNode(override val data: SbtBuildModuleData) extends Node[SbtBuildModuleData] {
  override protected def key: Key[SbtBuildModuleData] = SbtBuildModuleData.Key
}


abstract class Node[T] {
  private var children = Vector.empty[Node[?]]

  protected def key: Key[T]

  protected def data: T

  def add(node: Node[?]): Unit = {
    children :+= node
  }

  def addAll(nodes: Iterable[Node[?]]): Unit = {
    children ++= nodes
  }

  def toDataNode: DataNode[T] = toDataNode(None)

  private def toDataNode(parent: Option[DataNode[?]]): DataNode[T] = {
    data match {
      // upstream: the platform creates modules by their internal name, Consulo's ModuleDataService uses the external name
      case moduleData: ModuleData => moduleData.setExternalName(moduleData.getInternalName)
      case _ =>
    }
    val node = new DataNode[T](key, data, parent.orNull)
    (children ++ dependencyOrderNode).map(_.toDataNode(Some(node))).foreach(node.addChild)
    node
  }

  // names of the dependency targets are final only when the data nodes are created
  private def dependencyOrderNode: Option[Node[?]] = {
    val dependencies = children.collect { case dependency: OrderedDependencyNode => dependency }
    Option.when(dependencies.nonEmpty) {
      val orders = new java.util.HashMap[String, Integer]()
      val productionOnTest = new java.util.HashSet[String]()
      dependencies.foreach { dependency =>
        orders.put(dependency.orderKey, dependency.getOrder)
        dependency match {
          case moduleDependency: ModuleDependencyNode if moduleDependency.isProductionOnTestDependency =>
            productionOnTest.add(moduleDependency.orderKey)
          case _ =>
        }
      }
      new DependencyOrderNode(SbtDependencyOrderData(orders, productionOnTest))
    }
  }
}

object Node {

  import scala.language.implicitConversions

  implicit def node2data[T](node: Node[T]): T = node.data
}
