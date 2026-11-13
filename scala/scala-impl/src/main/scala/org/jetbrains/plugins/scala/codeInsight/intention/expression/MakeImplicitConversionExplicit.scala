package org.jetbrains.plugins.scala.codeInsight.intention.expression

import consulo.localize.LocalizeValue

import consulo.annotation.component.ExtensionImpl
import consulo.language.editor.intention.IntentionMetaData

import consulo.language.editor.intention.PsiElementBaseIntentionAction
import consulo.codeEditor.Editor
import consulo.project.Project
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.actions.MakeExplicitAction
import org.jetbrains.plugins.scala.extensions.{IteratorExt, ObjectExt, PsiElementExt}
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScBlock, ScExpression}
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction

@ExtensionImpl
@IntentionMetaData(ignoreId = "scala.MakeImplicitConversionExplicit", fileExtensions = Array("scala"), categories = Array("Scala", "Expressions"))
class MakeImplicitConversionExplicit extends PsiElementBaseIntentionAction {

  import MakeImplicitConversionExplicit._

  def getFamilyName: String = ScalaBundle.message("family.name.make.implicit.conversion.explicit")

  override def getText: LocalizeValue = LocalizeValue.of(getFamilyName)

  override def isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean =
    findImplicitElement(element).isDefined

  override def invoke(project: Project, editor: Editor, element: PsiElement): Unit =
    for {
      (expression, function) <- findImplicitElement(element)

      importStatically = expression.implicitConversions(fromUnderscore = true).contains(function)
    } MakeExplicitAction.replaceWithExplicit(expression, function, importStatically)(using project, editor)
}

object MakeImplicitConversionExplicit {
  private def findImplicitElement(element: PsiElement) = for {
    parent <- element.withParentsInFile.takeWhile(!_.is[ScBlock]).findByType[ScExpression]
    if parent.isValid

    function <- parent.implicitElement(fromUnderscore = true)
    if function.is[ScFunction]
  } yield (parent, function.asInstanceOf[ScFunction])

}