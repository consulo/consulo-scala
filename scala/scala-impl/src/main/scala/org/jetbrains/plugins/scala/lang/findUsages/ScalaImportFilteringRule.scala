package org.jetbrains.plugins.scala.lang.findUsages

import consulo.usage.rule.{ImportFilteringRule, PsiElementUsage}
import consulo.usage.{Usage, UsageTarget}
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports.ScImportExpr

final class ScalaImportFilteringRule extends ImportFilteringRule {

  override def isVisible(usage: Usage, targets: Array[UsageTarget]): Boolean = usage match {
    case p: PsiElementUsage =>
      p.getElement.nullSafe.forall { element =>
        element.containingScalaFile match {
          case Some(_) =>
            val isImport = element.parentsInFile.containsInstanceOf[ScImportExpr]
            !isImport
          case None => true
        }
      }
    case _ => true
  }
}