package org.jetbrains.plugins.scala.worksheet

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.psi.LanguageSubstitutor
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.plugins.scala.project.ModuleExt
import org.jetbrains.plugins.scala.worksheet.settings.WorksheetFileSettings

@ExtensionImpl
final class WorksheetLanguageSubstitutor extends LanguageSubstitutor {

  override def getLanguage: Language = WorksheetLanguage.INSTANCE

  override def getLanguage(file: VirtualFile, project: Project): Language = {
    val module = WorksheetFileSettings(project, file).getModule
    if (module.exists(_.hasScala3))
      WorksheetLanguage3.INSTANCE
    else
      null
  }
}
