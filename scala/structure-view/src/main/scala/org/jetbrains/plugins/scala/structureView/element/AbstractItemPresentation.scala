package org.jetbrains.plugins.scala.structureView.element

import consulo.ui.ex.ColoredItemPresentation
import consulo.codeEditor.CodeInsightColors
import consulo.colorScheme.TextAttributesKey
import consulo.application.dumb.IndexNotReadyException
import consulo.component.util.Iconable
import consulo.language.icon.IconDescriptorUpdaters
import consulo.localize.LocalizeValue
import consulo.ui.image.Image
import com.intellij.java.language.psi.PsiMember
import org.jetbrains.plugins.scala.extensions.PsiMemberExt
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScEnumCase

// TODO make private (after decoupling Test)
trait AbstractItemPresentation extends ColoredItemPresentation { self: Element =>

  // upstream: getPresentableText returns String, in Consulo it returns LocalizeValue
  def presentableText: String

  override def getPresentableText: LocalizeValue = LocalizeValue.of(presentableText)

  override def getIcon: Image =
    IconDescriptorUpdaters.getIcon(element, Iconable.ICON_FLAG_VISIBILITY)

  // upstream: ItemPresentation.getLocationString has a default implementation
  override def getLocationString: String = null

  override def getTextAttributesKey: TextAttributesKey =
    if (inherited) CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES
    else try {
      if (isDeprecated) CodeInsightColors.DEPRECATED_ATTRIBUTES
      else null
    } catch {
      case _: IndexNotReadyException => null // do not show deprecation info during indexing
    }

  private def isDeprecated: Boolean = element match {
    case enumCase: ScEnumCase       => enumCase.enumCases.isDeprecated
    case docCommentOwner: PsiMember => docCommentOwner.isDeprecated
    case _                          => false
  }
}

private object AbstractItemPresentation {
  private val FullyQualifiedName = "(?:\\w+\\.)+(\\w+)".r

  def withSimpleNames(presentation: String): String =
    FullyQualifiedName.replaceAllIn(presentation, "$1")
}
