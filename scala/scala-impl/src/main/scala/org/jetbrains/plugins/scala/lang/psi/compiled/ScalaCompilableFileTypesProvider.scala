package org.jetbrains.plugins.scala.lang.psi.compiled

import com.intellij.openapi.compiler.CompilableFileTypesProvider
import consulo.virtualFileSystem.fileType.FileType
import org.jetbrains.plugins.scala.api.ScalaFileType

import java.util

class ScalaCompilableFileTypesProvider extends CompilableFileTypesProvider {
  override def getCompilableFileTypes: util.Set[FileType] = util.Collections.singleton(ScalaFileType.INSTANCE)
}
