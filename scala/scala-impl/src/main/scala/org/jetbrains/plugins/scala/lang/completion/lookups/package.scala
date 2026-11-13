package org.jetbrains.plugins.scala.lang.completion

import consulo.language.editor.completion.lookup.LookupElementPresentation
import consulo.language.psi.PsiNamedElement
import com.intellij.util.IconUtil.getEmptyIcon
import com.siyeh.ig.psiutils.JavaDeprecationUtils.isDeprecated

package object lookups {

  // TODO extract as a customizable LookupElementRenderer
  private[completion] implicit class PresentationExt(private val presentation: LookupElementPresentation) extends AnyVal {

    def setStrikeout(element: PsiNamedElement): Unit =
      presentation.setStrikeout {
        isReal && isDeprecated(element, null)
      }

    def setIcon(element: PsiNamedElement): Unit = presentation.setIcon {
      if (isReal) element.getIcon(0)
      else getEmptyIcon(false)
    }

    def appendGrayedTailText(text: String): Unit =
      if (text != null && text.nonEmpty) {
        presentation.appendTailText(text, true)
      }

    private def isReal = true
  }
}
