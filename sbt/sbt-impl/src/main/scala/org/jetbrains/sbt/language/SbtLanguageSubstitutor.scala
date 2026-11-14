package org.jetbrains.sbt.language

import consulo.annotation.component.ExtensionImpl
import org.jetbrains.sbt.api.language.SbtLanguage


import org.jetbrains.sbt.api.language.SbtLanguageScala3
import consulo.language.Language
import consulo.language.util.ModuleUtilCore
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import consulo.language.psi.LanguageSubstitutor
import org.jetbrains.plugins.scala.project.ModuleExt

@ExtensionImpl
final class SbtLanguageSubstitutor extends LanguageSubstitutor {
  override def getLanguage: Language = SbtLanguage.INSTANCE

  override def getLanguage(file: VirtualFile, project: Project): Language = {
    val module = ModuleUtilCore.findModuleForFile(file, project)
    Option(module).flatMap(SbtBuildModuleSupport.findBuildModule(_, project)) match {
      case Some(buildModule) if buildModule.hasScala3 => SbtLanguageScala3.INSTANCE
      case _ => null
    }
  }
}
