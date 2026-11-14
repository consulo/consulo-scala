package org.jetbrains.sbt.project.settings

import consulo.module.Module
import consulo.util.dataholder.Key

import java.util.List as JList
import scala.beans.BeanProperty
import scala.compiletime.uninitialized

/**
 * In shared sources modules, it stores the module names of their owner modules.
 */
// upstream: a module-level PersistentStateComponent (module file storage), read by the JPS SharedSourcesModuleSerializer.
// Consulo has no module-level persistent services and no JPS - the value is kept in memory (module user data)
class SharedSourcesOwnerModules {

  @BeanProperty
  var ownersModuleNames: JList[String] = uninitialized

  def getState: SharedSourcesOwnerModules = this
}

object SharedSourcesOwnerModules {
  private val InstanceKey: Key[SharedSourcesOwnerModules] = Key.create("sbt.shared.sources.owner.modules")

  def getInstance(module: Module): SharedSourcesOwnerModules = synchronized {
    Option(module.getUserData(InstanceKey)).getOrElse {
      val instance = new SharedSourcesOwnerModules
      module.putUserData(InstanceKey, instance)
      instance
    }
  }
}
