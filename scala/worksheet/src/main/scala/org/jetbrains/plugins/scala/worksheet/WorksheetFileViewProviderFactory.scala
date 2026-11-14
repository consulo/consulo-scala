package org.jetbrains.plugins.scala
package worksheet

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.file.LanguageFileViewProviderFactory
import consulo.language.impl.file.SingleRootFileViewProvider
import consulo.language.psi.PsiManager
import consulo.virtualFileSystem.VirtualFile

abstract class WorksheetFileViewProviderFactoryBase extends LanguageFileViewProviderFactory {

  override def createFileViewProvider(
    file: VirtualFile,
    language: Language,
    manager: PsiManager,
    eventSystemEnabled: Boolean
  ): SingleRootFileViewProvider =
    new WorksheetFileViewProvider(manager, file, eventSystemEnabled, language)
}

@ExtensionImpl
final class WorksheetFileViewProviderFactory extends WorksheetFileViewProviderFactoryBase {
  override def getLanguage: Language = WorksheetLanguage.INSTANCE
}

@ExtensionImpl
final class WorksheetFileViewProviderFactory3 extends WorksheetFileViewProviderFactoryBase {
  override def getLanguage: Language = WorksheetLanguage3.INSTANCE
}

private final class WorksheetFileViewProvider(
  manager: PsiManager,
  file: VirtualFile,
  eventSystemEnabled: Boolean,
  language: Language,
) extends SingleRootFileViewProvider(
  manager,
  file,
  eventSystemEnabled,
  language
)
