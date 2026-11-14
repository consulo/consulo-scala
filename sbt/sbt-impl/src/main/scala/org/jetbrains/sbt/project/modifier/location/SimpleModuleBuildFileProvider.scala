package org.jetbrains.sbt.project.modifier.location

import consulo.module.Module
import org.jetbrains.plugins.scala.extensions.PathExt
import org.jetbrains.sbt.Sbt
import org.jetbrains.sbt.project.modifier.BuildFileElementType

import java.nio.file.Path

object SimpleModuleBuildFileProvider extends BuildFileProvider {

  override def findIoFile(module: Module, elementType: BuildFileElementType): Option[BuildFileEntry[Path]] = {
    // upstream: the directory of the module file
    val buildFile = Path.of(module.getModuleDirPath) / Sbt.BuildFile
    if (buildFile.exists) Some(BuildFileEntry(buildFile, isModuleLocal = true)) else None
  }
}
