package org.jetbrains.plugins.scala.actions

import consulo.dataContext.DataContext
import consulo.annotation.component.{ComponentScope, ExtensionAPI}
import consulo.component.extension.ExtensionPointName

// upstream: @ApiStatus.Internal
@ExtensionAPI(ComponentScope.APPLICATION)
trait NewScalaFileActionExtension {
  /**
   * This method is used only if the given `dataContext` contains a non-null module and if:
   *  - Either the default method [[com.intellij.ide.actions.CreateTemplateInPackageAction#isAvailable]] returns false for the given context, or
   *  - The module in the `dataContext` doesn't have Scala installed
   *
   * @see [[org.jetbrains.plugins.scala.actions.NewScalaFileAction#isAvailable]]
   */
  def isAvailable(dataContext: DataContext): Boolean
}

object NewScalaFileActionExtension {
  private val EpName = ExtensionPointName.create[NewScalaFileActionExtension](classOf[NewScalaFileActionExtension])

  def isAvailable(dataContext: DataContext): Boolean = {
    EpName.getExtensions.iterator.exists(_.isAvailable(dataContext))
  }
}