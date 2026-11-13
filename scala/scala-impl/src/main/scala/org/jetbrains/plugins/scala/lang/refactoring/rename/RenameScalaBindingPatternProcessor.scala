package org.jetbrains.plugins.scala.lang.refactoring.rename

import consulo.annotation.component.ExtensionImpl

import consulo.language.psi.PsiElement
import consulo.language.editor.refactoring.rename.RenamePsiElementProcessor
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScBindingPattern
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScClassParameter
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScValue, ScVariable}

@ExtensionImpl(order = "first")
class RenameScalaBindingPatternProcessor extends RenamePsiElementProcessor with ScalaRenameProcessor {
  override def canProcessElement(element: PsiElement): Boolean = element match {
    case pattern: ScBindingPattern =>
      pattern.nameContext match {
        case _: ScVariable | _: ScValue | _: ScClassParameter => false //handled by RenameScalaValsProcessor
        case _ => true
      }
    case _ => false
  }
}
