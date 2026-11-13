package org.jetbrains.plugins.scala.lang.refactoring.rename

import consulo.annotation.component.{ComponentScope, ExtensionAPI}
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.ExtensionPointDeclaration

import java.util

// upstream: @ApiStatus.Internal
@ExtensionAPI(ComponentScope.APPLICATION)
abstract class ScalaElementToRenameContributor {
  def addElements(original: PsiElement, newName: String, allRenames: util.Map[PsiElement, String]): Unit
}

object ScalaElementToRenameContributor
  extends ExtensionPointDeclaration[ScalaElementToRenameContributor](
    "org.intellij.scala.scalaElementToRenameContributor"
  ) {

  def addAllElements(original: PsiElement, newName: String, allRenames: util.Map[PsiElement, String]): Unit =
    implementations.foreach(_.addElements(original, newName, allRenames))
}