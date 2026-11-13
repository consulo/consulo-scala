package org.jetbrains.plugins.scala.lang.surroundWith.surrounders.expression

import consulo.document.util.TextRange
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.api.expr._

class ScalaWithTryFinallySurrounder extends ScalaWithTrySurrounderBase {
  override def getTemplateAsString(elements: Array[PsiElement]): String =
    "try {\n" + super.getTemplateAsString(elements) + "\n} finally a"

  //noinspection ScalaExtractStringToBundle,DialogTitleCapitalization
  override def getTemplateDescription = "try / finally"

  override protected def getRangeToDelete(tryStmt: ScTry): Option[TextRange] = for {
    block <- tryStmt.finallyBlock
    expr  <- block.expression.flatMap(_.forcePostprocessAndRestore)
  } yield expr.getTextRange
}
