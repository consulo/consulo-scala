package org.jetbrains.plugins.scala.project.settings

import consulo.annotation.component.{ComponentScope, ExtensionAPI}
import consulo.language.psi.PsiFile
import org.jetbrains.plugins.scala.ExtensionPointDeclaration

// upstream: @ApiStatus.Internal
@ExtensionAPI(ComponentScope.APPLICATION)
trait ScalaCompilerSettingsProfileProvider {

  def provide(file: PsiFile): Option[ScalaCompilerSettingsProfile]
}

object ScalaCompilerSettingsProfileProvider
  extends ExtensionPointDeclaration[ScalaCompilerSettingsProfileProvider](
    "org.intellij.scala.compilerSettingsProfileProvider"
  ) {

  def settingsFor(file: PsiFile): Option[ScalaCompilerSettingsProfile] =
    implementations.iterator.flatMap(_.provide(file)).nextOption()
}
