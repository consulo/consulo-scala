package org.jetbrains.plugins.scala.lang.resolve

import consulo.annotation.component.{ComponentScope, ExtensionAPI}
import com.intellij.java.language.psi.PsiClass
import consulo.language.psi.scope.GlobalSearchScope
import org.jetbrains.plugins.scala.ExtensionPointDeclaration

// upstream: @Internal
@ExtensionAPI(ComponentScope.APPLICATION)
abstract class SyntheticClassProducer {

  def findClasses(fqn: String, scope: GlobalSearchScope): Array[PsiClass]
}

object SyntheticClassProducer
  extends ExtensionPointDeclaration[SyntheticClassProducer](
    "org.intellij.scala.scalaSyntheticClassProducer"
  ) {


  def getAllClasses(fqn: String, scope: GlobalSearchScope): Array[PsiClass] =
    implementations.toArray.flatMap(_.findClasses(fqn, scope))
}