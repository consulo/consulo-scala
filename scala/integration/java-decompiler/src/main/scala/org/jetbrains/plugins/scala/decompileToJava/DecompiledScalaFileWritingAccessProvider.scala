package org.jetbrains.plugins.scala
package decompileToJava

import java.{util => ju}

import consulo.annotation.component.ExtensionImpl
import consulo.virtualFileSystem.{VirtualFile, WritingAccessProvider}

// upstream: decompiled files are recognized by their DummyFileSystem folder; here they are marked with a user data key
@ExtensionImpl
class DecompiledScalaFileWritingAccessProvider extends WritingAccessProvider {

  override def isPotentiallyWritable(file: VirtualFile): Boolean =
    !ScalaBytecodeDecompileTask.isDecompiled(file)

  override def requestWriting(virtualFiles: VirtualFile*): ju.Collection[VirtualFile] =
    ju.Collections.emptyList()
}
