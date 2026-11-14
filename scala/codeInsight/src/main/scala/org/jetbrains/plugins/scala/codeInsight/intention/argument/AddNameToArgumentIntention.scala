package org.jetbrains.plugins.scala
package codeInsight
package intention
package argument

import consulo.annotation.component.ExtensionImpl
import consulo.language.editor.intention.IntentionMetaData

import consulo.language.editor.intention.PsiElementBaseIntentionAction
import consulo.codeEditor.Editor
import consulo.project.Project
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle

/**
 * Jason Zaugg
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "scala.AddNameToArgumentIntention", fileExtensions = Array("scala"), categories = Array("Scala", "Argument conversion"))
final class AddNameToArgumentIntention extends PsiElementBaseIntentionAction {

  override def isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean =
    addNameToArgumentsFix(element, onlyBoolean = false).isDefined

  override def invoke(project: Project, editor: Editor, element: PsiElement): Unit = {
    if (!element.isValid) return
    addNameToArgumentsFix(element, onlyBoolean = false).foreach(_.apply())
  }

  override def getFamilyName: String = ScalaCodeInsightBundle.message("family.name.use.named.arguments")

  override def getText: String = ScalaCodeInsightBundle.message("use.named.arguments.for.current.and.subsequent.arguments")
}
