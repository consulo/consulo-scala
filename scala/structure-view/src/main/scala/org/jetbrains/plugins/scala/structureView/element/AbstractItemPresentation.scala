package org.jetbrains.plugins.scala.structureView.element

import com.intellij.navigation.ColoredItemPresentation
import consulo.codeEditor.CodeInsightColors
import consulo.colorScheme.TextAttributesKey
import consulo.application.dumb.IndexNotReadyException
import consulo.component.util.Iconable
import com.intellij.java.language.psi.PsiMember
import org.jetbrains.plugins.scala.extensions.PsiMemberExt
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScEnumCase

import javax.swing.*

// TODO make private (after decoupling Test)
trait AbstractItemPresentation extends ColoredItemPresentation { self: Element =>

  override def getPresentableText: String

  override def getIcon(open: Boolean): Icon =
    element.getIcon(Iconable.ICON_FLAG_VISIBILITY)

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
