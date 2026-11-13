package org.jetbrains.plugins.scala.lang.psi.stubs.index

import consulo.project.Project
import com.intellij.java.language.psi.PsiClass
import consulo.language.psi.scope.GlobalSearchScope
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScValue
import org.jetbrains.plugins.scala.lang.psi.stubs.index.ScalaIndexKeys.StubIndexStringKeyExt
import org.jetbrains.plugins.scala.lang.psi.types.ScTypeExt

object StableValIndex {
  def forClassFqn(qualifiedName: String, scope: GlobalSearchScope)
                 (implicit project: Project): Set[ScValue] =
    ScalaIndexKeys.PROPERTY_CLASS_NAME_KEY
      .forClassFqn(qualifiedName, scope)
      .filterByType[ScValue]

  def findValuesOfClassType(c: PsiClass, scope: GlobalSearchScope): Set[ScValue] = {
    val className = c.qualifiedName
    forClassFqn(className, scope)(using c.getProject)
      .filter {
        case v: ScValue if v.declaredElements.size == 1 =>
          v.`type`().toOption
            .flatMap(_.extractClass)
            .map(_.qualifiedName)
            .contains(className)
        case _ => false
      }
  }
}
