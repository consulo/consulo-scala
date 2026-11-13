package org.jetbrains.plugins.scala.settings

import consulo.component.persist.{PersistentStateComponent, State, Storage}
import consulo.project.Project

import scala.beans.BooleanBeanProperty

@State(
  name     = "CompilerIndicesSettings",
  storages = Array(new Storage("compiler_indices_settings.xml"))
)
class CompilerIndicesSettings(project: Project) extends PersistentStateComponent[CompilerIndicesSettings.State] {
  private var state: CompilerIndicesSettings.State = new CompilerIndicesSettings.State

  /** Corresponds to the actual value set in configurable, consider using
    * [[isBytecodeIndexingActive]] to check if the indexing is explicitly enabled AND
    * makes sense in the context of the current project (i.e. this is a project built
    * with sbt shell or IDEA's JPS builder).
    */
  def isIndexingEnabled: Boolean                   = state.indexingEnabled
  def isEnabledForImplicitDefs: Boolean            = state.enabledForImplicitDefs
  def isEnabledForApplyUnapply: Boolean            = state.enabledForApplyUnapply
  def isEnabledForSAMTypes: Boolean                = state.enabledForSAMTypes
  def isEnabledForForComprehensionMethods: Boolean = state.enabledForForCompMethods

  def setEnabledForImplicitDefs(enabled:            Boolean): Unit = state.enabledForImplicitDefs = enabled
  def setEnabledForApplyUnapply(enabled:            Boolean): Unit = state.enabledForApplyUnapply = enabled
  def setEnabledForSAMTypes(enabled:                Boolean): Unit = state.enabledForSAMTypes = enabled
  def setEnabledForForComprehensionMethods(enabled: Boolean): Unit = state.enabledForForCompMethods = enabled

  def setIndexingEnabled(v: Boolean): Unit = {
    state.indexingEnabled = v
  }

  // upstream: checks that the build is run by JPS or sbt shell project task runner (ProjectTaskManager),
  // Consulo has no project task runners, the build is always run by the Consulo compiler
  private def hasCompatibleRunner: Boolean = true

  def isBytecodeIndexingActive: Boolean = isIndexingEnabled && hasCompatibleRunner

  override def getState: CompilerIndicesSettings.State                = state
  override def loadState(loaded: CompilerIndicesSettings.State): Unit = state = loaded
}

object CompilerIndicesSettings {
  class State {
    @BooleanBeanProperty var indexingEnabled: Boolean          = true
    @BooleanBeanProperty var enabledForImplicitDefs: Boolean   = true
    @BooleanBeanProperty var enabledForApplyUnapply: Boolean   = true
    @BooleanBeanProperty var enabledForSAMTypes: Boolean       = true
    @BooleanBeanProperty var enabledForForCompMethods: Boolean = true
  }

  def apply(project: Project): CompilerIndicesSettings =
    project.getInstance(classOf[CompilerIndicesSettings])
}
