package org.jetbrains.plugins.scala.lang.refactoring.rename

import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.refactoring.rename.VetoRenameCondition
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.api.base.ScEnd
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScGivenPattern

/**
 * This condition is called when no available RenameHandler is found.
 * Normally the default rename handler is used then.
 * This class can prevent that and prevent any renaming on the given element.
 */
@ExtensionImpl
class ScalaVetoDefaultRenameCondition extends VetoRenameCondition {
  override def isVetoed(t: PsiElement): Boolean = shouldNotRename(t)

  private def shouldNotRename(element: PsiElement): Boolean = element match {
    case _: ScEnd =>
      // We get ScEnd here if the cursor is located on the actual end-token of an end marker
      // In that case we don't want to rename anything.
      // This is in line with other tokens like `package`, `class`, `def`, etc.
      true
    case _: ScGivenPattern =>
      // The name of a given pattern is synthesized from its type (see ScalaPsiUtil.generateGivenName)
      // and, unlike a given definition, a pattern cannot be given an explicit name.
      // So there is no way to rename it.
      true
    case _ =>
      false
  }
}
