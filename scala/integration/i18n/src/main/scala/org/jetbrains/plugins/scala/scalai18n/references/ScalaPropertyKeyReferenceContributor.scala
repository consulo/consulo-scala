package org.jetbrains.plugins.scala.scalai18n.references

import com.intellij.lang.properties.references.PropertyReference
import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.pattern.PlatformPatterns
import consulo.language.psi.{PsiElement, PsiReference, PsiReferenceContributor, PsiReferenceProvider, PsiReferenceRegistrar}
import consulo.language.util.ProcessingContext
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.lang.psi.api.base.literals.ScStringLiteral
import org.jetbrains.plugins.scala.scalai18n.codeInspection.i18n.ScalaI18nUtil.PropertyReferenceResolver

// upstream: property key references in Scala strings come from the platform's UAST based I18nReferenceContributor
@ExtensionImpl
final class ScalaPropertyKeyReferenceContributor extends PsiReferenceContributor {

  override def registerReferenceProviders(registrar: PsiReferenceRegistrar): Unit =
    registrar.registerReferenceProvider(PlatformPatterns.psiElement(classOf[ScStringLiteral]), new ScalaPropertyKeyReferenceProvider)

  override def getLanguage: Language = ScalaLanguage.INSTANCE
}

final class ScalaPropertyKeyReferenceProvider extends PsiReferenceProvider {

  override def getReferencesByElement(element: PsiElement, context: ProcessingContext): Array[PsiReference] = element match {
    case literal: ScStringLiteral if literal.getValue != null =>
      val resolver = PropertyReferenceResolver(literal)
      resolver.bundleName match {
        case Some(bundleName) => Array(new PropertyReference(resolver.key, literal, bundleName, false))
        case None => PsiReference.EMPTY_ARRAY
      }
    case _ => PsiReference.EMPTY_ARRAY
  }
}
