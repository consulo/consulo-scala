package org.jetbrains.plugins.scala.codeInsight.intention.types

import consulo.annotation.component.ExtensionImpl
import consulo.language.editor.intention.IntentionMetaData

import consulo.language.editor.intention.PsiElementBaseIntentionAction
import consulo.codeEditor.Editor
import consulo.application.dumb.DumbAware
import consulo.project.Project
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.codeInspection.quickfix.ConvertFromInfixTypeQuickFix
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.api.base.ScStableCodeReference
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScInfixTypeElement

/** Converts type element `(A @@ B)` to `@@[A, B]` */
@ExtensionImpl
@IntentionMetaData(ignoreId = "scala.ConvertFromInfixIntention", fileExtensions = Array("scala"), categories = Array("Scala", "Type"))
class ConvertFromInfixIntention extends PsiElementBaseIntentionAction with DumbAware {
  override def getFamilyName: String = ScalaBundle.message("family.name.use.prefix.type.syntax")

  override def getText: String = getFamilyName

  override def isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean = {
    element match {
      case Parent((_: ScStableCodeReference) & Parent(Parent(_: ScInfixTypeElement))) => true
      case _ => false
    }
  }

  override def invoke(project: Project, editor: Editor, element: PsiElement): Unit = {
    val infixTypeElement = Option(element).filter(_.isValid)
      .flatMap(_.parentOfType(classOf[ScInfixTypeElement], strict = false))
      .getOrElse(return)

    ConvertFromInfixTypeQuickFix.applyFix(infixTypeElement)
  }
}
