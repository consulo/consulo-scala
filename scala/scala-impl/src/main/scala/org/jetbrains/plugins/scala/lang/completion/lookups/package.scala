package org.jetbrains.plugins.scala.lang.completion

import consulo.language.editor.completion.lookup.LookupElementPresentation
import consulo.language.psi.PsiNamedElement
import consulo.language.icon.IconDescriptorUpdaters
import consulo.ui.image.Image
import com.siyeh.ig.psiutils.JavaDeprecationUtils.isDeprecated

package object lookups {

  // TODO extract as a customizable LookupElementRenderer
  private[completion] implicit class PresentationExt(private val presentation: LookupElementPresentation) extends AnyVal {

    def setStrikeout(element: PsiNamedElement): Unit =
      presentation.setStrikeout {
        isReal && isDeprecated(element, null)
      }

    def setIcon(element: PsiNamedElement): Unit = presentation.setIcon {
      if (isReal) IconDescriptorUpdaters.getIcon(element, 0)
      else Image.empty(Image.DEFAULT_ICON_SIZE)
    }

    def appendGrayedTailText(text: String): Unit =
      if (text != null && text.nonEmpty) {
        presentation.appendTailText(text, true)
      }

    private def isReal = true
  }
}
