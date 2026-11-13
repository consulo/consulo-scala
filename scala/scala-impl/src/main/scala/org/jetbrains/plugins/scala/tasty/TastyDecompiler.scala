package org.jetbrains.plugins.scala.tasty

import consulo.annotation.component.ExtensionImpl

import consulo.virtualFileSystem.BinaryFileDecompiler
import consulo.virtualFileSystem.VirtualFile

/**
 * Implementation of IntelliJ decompiler interface ([[BinaryFileDecompiler]])
 * that presents Tasty file contents as a Scala source code with outlines.
 *
 * @see [[org.jetbrains.plugins.scala.tasty.reader.TreePrinter]] ~ printing Tasty content as Scala source code
 * @see [[org.jetbrains.plugins.scala.tasty.reader.TreeReader]] ~ reading Tasty contnet to internal Node represenataion
 */
@ExtensionImpl
class TastyDecompiler extends BinaryFileDecompiler {
  override def decompile(file: VirtualFile): String =
    TastyReader.read(file.contentsToByteArray())
      .map(_._2)
      .getOrElse(throw new RuntimeException(s"Error reading TASTy file: ${file.getPath}"))
}
