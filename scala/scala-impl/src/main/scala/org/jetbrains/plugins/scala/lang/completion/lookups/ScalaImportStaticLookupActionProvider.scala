package org.jetbrains.plugins.scala.lang.completion.lookups

import consulo.annotation.component.ExtensionImpl

import consulo.localize.LocalizeValue
import consulo.language.editor.completion.lookup.{Lookup, LookupActionProvider, LookupElement, LookupElementAction}
import com.intellij.java.language.psi.PsiClass
import consulo.language.psi.PsiNamedElement
import consulo.platform.base.icon.PlatformIconGroup
import consulo.ui.image.Image
import java.util.function.Consumer
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.extensions.ObjectExt
import org.jetbrains.plugins.scala.lang.completion.handlers.ScalaImportingInsertHandler
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScReferenceExpression

@ExtensionImpl
final class ScalaImportStaticLookupActionProvider extends LookupActionProvider {

  override def fillActions(element: LookupElement,
                           lookup: Lookup,
                           consumer: Consumer[LookupElementAction]): Unit = element match {
    case element: ScalaLookupItem if element.isClassName &&
      element.getInsertHandler == null &&
      !element.getPsiElement.is[PsiClass] =>

      val checkIcon: Image = PlatformIconGroup.actionsChecked()
      val icon = if (element.shouldImport)
        Image.empty(checkIcon.getWidth, checkIcon.getHeight)
      else
        checkIcon

      consumer.accept(new LookupElementAction(icon, LocalizeValue.of(ScalaBundle.message("action.import.member"))) {

        import LookupElementAction.Result.ChooseItem

        override def performLookupAction: ChooseItem = {
          val handler = new ScalaImportStaticLookupActionProvider.BindingInsertHandler(
            element.getPsiElement,
            element.containingClass
          )
          element.setInsertHandler(handler)
          new ChooseItem(element)
        }
      })
    case _ =>
  }
}

object ScalaImportStaticLookupActionProvider {

  private class BindingInsertHandler(private val targetElement: PsiNamedElement,
                                     override protected val containingClass: PsiClass)
    extends ScalaImportingInsertHandler(containingClass) {

    override protected def qualifyAndImport(reference: ScReferenceExpression): Unit =
      reference.bindToElement(
        targetElement,
        Some(containingClass)
      )
  }

}