package org.jetbrains.plugins.scala.compiler

import org.jetbrains.jps.incremental.scala.shared.remote.PathTranslator
import org.jetbrains.plugins.scala.extensions.PathExt

import java.nio.file.{FileSystems, Path}

object EelPathTranslator extends PathTranslator {
  /**
   * Translates the path to be local within the remote environment.
   * If the path is already local, it is returned as is; a path of a remote platform file system
   * (`platform.fs()`) is rendered as the native path on that machine.
   */
  override def translate(path: Path): String =
    if (path.getFileSystem == FileSystems.getDefault) path.toString
    else path.toCanonicalPath.toString
}
