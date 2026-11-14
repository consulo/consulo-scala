package org.jetbrains.bsp.project

import consulo.annotation.component.ComponentScope
import consulo.annotation.component.ServiceAPI
import consulo.annotation.component.ServiceImpl
import jakarta.inject.Singleton
import consulo.component.persist.{PersistentStateComponent, State, Storage, StoragePathMacros}
import consulo.project.Project
import consulo.util.xml.serializer.XmlSerializerUtil

import java.util.Collections
import scala.beans.BeanProperty

@State(
  name = "BspTargetCanCompile",
  storages = Array(new Storage(StoragePathMacros.WORKSPACE_FILE))
)
// upstream: @Service(Array(Service.Level.PROJECT))
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
final class BspTargetCanCompile
  extends PersistentStateComponent[BspTargetCanCompile] {

  @BeanProperty
  var compilableTargets: java.util.List[String] = Collections.emptyList()

  override def getState: BspTargetCanCompile = this

  override def loadState(state: BspTargetCanCompile): Unit = XmlSerializerUtil.copyBean(state, this)
}

object BspTargetCanCompile {
  def getInstance(project: Project): BspTargetCanCompile = {
    project.getInstance(classOf[BspTargetCanCompile])
  }
}