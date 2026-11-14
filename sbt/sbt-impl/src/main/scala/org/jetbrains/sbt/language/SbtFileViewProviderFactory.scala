package org.jetbrains.sbt
package language

import consulo.annotation.component.ExtensionImpl
import org.jetbrains.sbt.api.language.SbtLanguage


import org.jetbrains.sbt.api.file
import consulo.language.Language
import consulo.virtualFileSystem.VirtualFile
import consulo.language.file.FileViewProviderFactory
import consulo.language.psi.PsiManager
import consulo.language.impl.file.SingleRootFileViewProvider

@ExtensionImpl
final class SbtFileViewProviderFactory extends FileViewProviderFactory {
  override def getLanguage: Language = SbtLanguage.INSTANCE


  override def createFileViewProvider(file: VirtualFile,
                                      language: Language,
                                      manager: PsiManager,
                                      eventSystemEnabled: Boolean): SingleRootFileViewProvider =
    new SbtFileViewProvider(manager, file, eventSystemEnabled, language)
}
