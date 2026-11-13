package org.jetbrains.sbt.api.project
import consulo.annotation.component.{ComponentScope, ExtensionAPI}
import consulo.application.Application
import consulo.module.Module

import java.net.URI
import java.nio.file.Path
import scala.jdk.CollectionConverters.IteratorHasAsScala

// upstream: @ApiStatus.Internal
// upstream: @ApiStatus.Experimental
// upstream: <extensionPoint qualifiedName="org.intellij.sbt.buildModuleUriProvider"/> in sbt-api.xml
@ExtensionAPI(ComponentScope.APPLICATION)
trait SbtBuildModuleUriProvider {
  /**
   * For a given `module` this method returns URI of it's corresponding build module.<br>
   * For example in this project structure {{{
   *   <root> //~ corresponds to IntelliJ module `root`
   *     |-- project //~ corresponds to IntelliJ module `root-build`
   *     |-- subProject1 //~ corresponds to IntelliJ module `subProject1`
   *     |-- subProject2 //~ corresponds to IntelliJ module `subProject2`
   *     |-- build.sbt
   * }}}
   * for modules `root`, `subProject1` and `subProject2` it will return URI or `root`<br>
   * for module `root-build` (corresponding to a build module itself) it will return `None`
   */
  def getBuildModuleUri(module: Module): Option[URI]


  /**
   * Same as `getBuildModuleUri` but returns a base directory instead of URI
   * (read about the difference in `org.jetbrains.sbt.project.data.SbtModuleData`)
   */
  def getBuildModuleBaseDirectory(module: Module): Option[Path]
}

object SbtBuildModuleUriProvider {

  def getBuildModuleUri(module: Module): Option[URI] = {
    val implementations = Application.get.getExtensionList(classOf[SbtBuildModuleUriProvider]).iterator()
    implementations.asScala.flatMap(_.getBuildModuleUri(module)).nextOption()
  }

  def getBuildModuleBaseDirectory(module: Module): Option[Path] = {
    val implementations = Application.get.getExtensionList(classOf[SbtBuildModuleUriProvider]).iterator()
    implementations.asScala.flatMap(_.getBuildModuleBaseDirectory(module)).nextOption()
  }
}
