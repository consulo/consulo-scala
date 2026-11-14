package org.jetbrains.sbt.project.settings

import consulo.annotation.component.ComponentScope
import consulo.annotation.component.ServiceAPI
import consulo.annotation.component.ServiceImpl
import jakarta.inject.Singleton

import consulo.component.persist.{PersistentStateComponent, State, Storage, StoragePathMacros}
import consulo.module.Module
import consulo.util.xml.serializer.XmlSerializerUtil

import scala.compiletime.uninitialized

@State(
  name = "DisplayModuleName",
  storages = Array(new Storage(StoragePathMacros.MODULE_FILE))
)
@Singleton
@ServiceAPI(ComponentScope.MODULE)
@ServiceImpl
class DisplayModuleName extends PersistentStateComponent[DisplayModuleName] {
  private var _name: String = uninitialized
  def name: String = _name

  def setName(name: String): Unit =
    this._name = name
  def getName: String =
    this._name

  override def getState: DisplayModuleName = this

  override def loadState(state: DisplayModuleName): Unit = XmlSerializerUtil.copyBean(state, this)
}

object DisplayModuleName {
  def getInstance(module: Module): DisplayModuleName =
    module.getInstance(classOf[DisplayModuleName])
}
