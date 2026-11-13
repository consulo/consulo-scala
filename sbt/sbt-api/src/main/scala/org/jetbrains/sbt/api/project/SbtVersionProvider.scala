package org.jetbrains.sbt.api.project
import consulo.annotation.component.{ComponentScope, ExtensionAPI}
import consulo.application.Application
import consulo.module.Module

import scala.jdk.CollectionConverters.IteratorHasAsScala

// upstream: @ApiStatus.Internal
// upstream: @ApiStatus.Experimental
// upstream: <extensionPoint qualifiedName="org.intellij.sbt.sbtVersionProvider"/> in sbt-api.xml
@ExtensionAPI(ComponentScope.APPLICATION)
trait SbtVersionProvider {
  def getSbtVersion(module: Module): Option[String]
}

object SbtVersionProvider {
  def getSbtVersion(module: Module): Option[String] = {
    val implementations = Application.get.getExtensionList(classOf[SbtVersionProvider]).iterator()
    implementations.asScala.flatMap(_.getSbtVersion(module)).nextOption()
  }
}
