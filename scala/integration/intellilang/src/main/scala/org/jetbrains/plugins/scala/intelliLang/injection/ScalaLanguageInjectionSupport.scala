package org.jetbrains.plugins.scala.intelliLang.injection

import consulo.annotation.component.ExtensionImpl

import consulo.language.inject.advanced.{AbstractLanguageInjectionSupport, BaseInjection, TemporaryPlacesRegistry}
import consulo.language.psi.{PsiElement, PsiLanguageInjectionHost}
import consulo.util.lang.ref.SimpleReference
import org.jetbrains.plugins.scala.lang.psi.api.base.ScLiteral
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScExpression, ScInfixExpr}
import org.jetbrains.plugins.scala.patterns.ScalaPatterns

@ExtensionImpl
final class ScalaLanguageInjectionSupport extends AbstractLanguageInjectionSupport {
  override def getId: String = ScalaLanguageInjectionSupport.Id

  override def isApplicableTo(host: PsiLanguageInjectionHost): Boolean = host.isInstanceOf[ScLiteral] || host.isInstanceOf[ScInfixExpr]

  override def getPatternClasses: Array[Class[?]] = Array(classOf[ScalaPatterns])

  override def useDefaultInjector(host: PsiLanguageInjectionHost): Boolean = false

  // upstream: useDefaultCommentInjector = false, ScalaLanguageInjector handles the comments itself
  override def findCommentInjection(host: PsiElement, commentRef: SimpleReference[PsiElement]): BaseInjection = null

  override def removeInjectionInPlace(host: PsiLanguageInjectionHost): Boolean = {
    if (!host.isInstanceOf[ScExpression])
      return false
    val project = host.getProject
    val registry = TemporaryPlacesRegistry.getInstance(project)
    if (registry == null)
      return false
    registry.removeHostWithUndo(project, host)
  }
}

object ScalaLanguageInjectionSupport {
  val Id = "scala"
}