package org.jetbrains.plugins.scala.testingSupport

import consulo.annotation.component.{ComponentScope, ExtensionAPI}
import consulo.module.Module
import org.jetbrains.plugins.scala.ExtensionPointDeclaration

@ExtensionAPI(ComponentScope.APPLICATION)
abstract class TestWorkingDirectoryProvider {
  def getWorkingDirectory(module: Module): Option[String]
}

object TestWorkingDirectoryProvider extends ExtensionPointDeclaration[TestWorkingDirectoryProvider](
  "org.intellij.scala.testWorkingDirectoryProvider"
)
