package org.jetbrains.plugins.scala
package decompileToJava

import consulo.annotation.component.ExtensionImpl

import java.{util => ju}

import com.intellij.openapi.vfs.ex.dummy.DummyFileSystem
import consulo.virtualFileSystem.VirtualFile
import com.intellij.openapi.vfs.WritingAccessProvider

@ExtensionImpl
class DecompiledScalaFileWritingAccessProvider extends WritingAccessProvider {

  override def isPotentiallyWritable(file: VirtualFile): Boolean =
    file.getFileSystem match {
      case _: DummyFileSystem =>
        val isDecompiled = Option(file.getParent)
          .map(_.getName)
          .contains(ScalaBytecodeDecompileTask.scalaDecompiledFolder)
        !isDecompiled
      case _ => true
    }

  override def requestWriting(virtualFiles: VirtualFile*): ju.Collection[VirtualFile] =
    ju.Collections.emptyList()
}
