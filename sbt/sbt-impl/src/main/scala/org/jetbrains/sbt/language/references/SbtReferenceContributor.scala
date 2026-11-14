package org.jetbrains.sbt
package language.references

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import org.jetbrains.sbt.api.language.SbtLanguage

import consulo.language.pattern.PlatformPatterns
import consulo.language.psi.{PsiReferenceContributor, PsiReferenceRegistrar}
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScReferencePattern

@ExtensionImpl(order = "before InjectedReferencesContributor, before CommentsReferenceContributor")
class SbtReferenceContributor extends PsiReferenceContributor {
  override def getLanguage: Language = SbtLanguage.INSTANCE

  override def registerReferenceProviders(registrar: PsiReferenceRegistrar): Unit = {
    registrar.registerReferenceProvider(PlatformPatterns.psiElement(classOf[ScReferencePattern]), new SbtSubprojectReferenceProvider())
  }
}

