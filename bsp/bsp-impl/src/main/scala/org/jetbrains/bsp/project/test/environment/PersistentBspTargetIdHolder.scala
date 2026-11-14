package org.jetbrains.bsp.project.test.environment

import consulo.annotation.component.ComponentScope
import consulo.annotation.component.ServiceAPI
import consulo.annotation.component.ServiceImpl
import jakarta.inject.Singleton


import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import consulo.component.persist.{PersistentStateComponent, State, Storage, StoragePathMacros}
import consulo.module.Module
import consulo.util.xml.serializer.XmlSerializerUtil
import org.jetbrains.bsp.project.test.environment.BspJvmEnvironment._

import scala.beans.BeanProperty

@State(
  name = "PersistentBspTargetIdHolder",
  storages = Array(new Storage(StoragePathMacros.MODULE_FILE))
)
@Singleton
@ServiceAPI(ComponentScope.MODULE)
@ServiceImpl
class PersistentBspTargetIdHolder
  extends PersistentStateComponent[PersistentBspTargetIdHolder]
    with BspTargetIdHolder {

  @BeanProperty var selected: String = scala.compiletime.uninitialized

  override def loadState(t: PersistentBspTargetIdHolder): Unit = {
    XmlSerializerUtil.copyBean(t, this)
  }

  override def getState: PersistentBspTargetIdHolder = this

  override def currentValue: Option[BuildTargetIdentifier] = Option(selected).map(new BuildTargetIdentifier(_))

  override def update(value: BuildTargetIdentifier): Unit = {
    selected = value.getUri
  }
}

object PersistentBspTargetIdHolder {
  def getInstance(module: Module): BspTargetIdHolder = {
    module.getInstance(classOf[PersistentBspTargetIdHolder])
  }
}
