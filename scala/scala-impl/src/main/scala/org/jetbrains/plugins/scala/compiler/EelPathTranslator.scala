package org.jetbrains.plugins.scala.compiler

import org.jetbrains.jps.incremental.scala.shared.remote.PathTranslator

import java.nio.file.Path

object EelPathTranslator extends PathTranslator {
  /**
   * Translates the path to be local within the remote environment.
   * Consulo runs the compile server on the local platform, so the path is returned as is.
   */
  override def translate(path: Path): String = path.toString
}
