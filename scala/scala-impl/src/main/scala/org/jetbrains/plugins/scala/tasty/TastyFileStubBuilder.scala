package org.jetbrains.plugins.scala.tasty

import com.intellij.java.language.impl.psi.impl.compiled.ClassFileStubBuilder
import consulo.application.Application
import consulo.virtualFileSystem.fileType.FileType
import jakarta.inject.Inject

// upstream: `filetype.stubBuilder` registration of ClassFileStubBuilder for TASTy files
class TastyFileStubBuilder @Inject()(application: Application) extends ClassFileStubBuilder(application) {
  override def getFileType: FileType = TastyFileType
}
