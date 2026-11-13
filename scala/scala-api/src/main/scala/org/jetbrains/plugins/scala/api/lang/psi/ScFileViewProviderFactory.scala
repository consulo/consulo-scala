package org.jetbrains.plugins.scala.api
package lang.psi

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.file.LanguageFileViewProviderFactory
import consulo.language.psi.PsiManager
import consulo.virtualFileSystem.VirtualFile

// upstream: registered in plugin.xml as <lang.fileViewProviderFactory language="Scala"/>
@ExtensionImpl
final class ScFileViewProviderFactory extends LanguageFileViewProviderFactory {

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def createFileViewProvider(file: VirtualFile, language: Language,
                                      manager: PsiManager, eventSystemEnabled: Boolean): org.jetbrains.plugins.scala.api.lang.psi.ScFileViewProvider =
    new ScFileViewProvider(manager, file, eventSystemEnabled, language)
}
