package org.jetbrains.plugins.scala.lang.psi.impl.toplevel

import consulo.annotation.component.ExtensionImpl

import consulo.language.psi.{ElementDescriptionLocation, ElementDescriptionProvider, PsiElement}
import consulo.localize.LocalizeValue
import consulo.usage.{UsageViewLongNameLocation, UsageViewShortNameLocation}
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiPresentationUtils.{extensionMethodPresentableText, extensionMethodShortText}
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction

/**
 * Provides receiver-qualified Scala 3 extension labels to the Usage View,
 * used by Find Usages and Show Usages. Long labels include signature clauses;
 * short labels do not, matching the respective ordinary-method conventions.
 */
@ExtensionImpl(order = "first")
final class ScalaExtensionMethodElementDescriptionProvider extends ElementDescriptionProvider {
  override def getElementDescription(element: PsiElement, location: ElementDescriptionLocation): LocalizeValue =
    element match {
      case function: ScFunction if function.isExtensionMethod && location == UsageViewLongNameLocation.INSTANCE =>
        LocalizeValue.ofNullable(extensionMethodPresentableText(function))
      case function: ScFunction if function.isExtensionMethod && location == UsageViewShortNameLocation.INSTANCE =>
        LocalizeValue.ofNullable(extensionMethodShortText(function))
      case _ => LocalizeValue.empty()
    }
}
