package org.jetbrains.plugins.scala.tasty

import consulo.annotation.component.ExtensionImpl

import consulo.language.Language
import consulo.virtualFileSystem.VirtualFile
import consulo.language.file.{FileViewProvider, FileViewProviderFactory}
import consulo.language.psi.PsiManager
import org.jetbrains.plugins.scala.api.Scala3Language
import org.jetbrains.plugins.scala.lang.psi.compiled.ScClassFileDecompiler

@ExtensionImpl
class TastyFileViewProviderFactory extends FileViewProviderFactory {
  override def createFileViewProvider(file: VirtualFile, language: Language, manager: PsiManager, eventSystemEnabled: Boolean): FileViewProvider = {
    ScClassFileDecompiler.createFileViewProviderImpl(manager, file, eventSystemEnabled, Scala3Language.INSTANCE)
  }
}