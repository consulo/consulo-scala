package org.jetbrains.idea.devkit.scala

import com.intellij.java.language.psi.PsiMethod
import consulo.annotation.access.RequiredReadAction
import consulo.annotation.component.ExtensionImpl
import consulo.codeEditor.markup.GutterIconRenderer
import consulo.devkit.icon.{IconLibraryChecker, IconLibraryGroupImageCache, IconLibraryLineMarkerProvider}
import consulo.language.Language
import consulo.language.editor.Pass
import consulo.language.editor.gutter.{GutterIconNavigationHandler, LineMarkerInfo, LineMarkerProvider}
import consulo.language.psi.{PsiElement, PsiFile}
import consulo.navigation.OpenFileDescriptorFactory
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScReferenceExpression

import consulo.ui.event.ComponentEvent

/**
 * Shows the icon of `MyIconGroup.someIcon()` references in Scala code in the gutter,
 * like consulo-devkit's IconLibraryLineMarkerProvider for Java.
 */
@ExtensionImpl
final class ScalaIconLibraryLineMarkerProvider extends LineMarkerProvider {

  @RequiredReadAction
  override def isAvailable(file: PsiFile): Boolean = IconLibraryChecker.containsImageApi(file)

  @RequiredReadAction
  override def getLineMarkerInfo(element: PsiElement): LineMarkerInfo[?] = element.getParent match {
    case ref: ScReferenceExpression if ref.nameId == element && isIconGroupQualifier(ref) =>
      ref.resolve() match {
        case method: PsiMethod => create(element, method)
        case _ => null
      }
    case _ => null
  }

  private def isIconGroupQualifier(ref: ScReferenceExpression): Boolean = ref.qualifier.exists {
    case qualifier: ScReferenceExpression => qualifier.refName.endsWith(IconLibraryLineMarkerProvider.ICON_GROUP_SUFFIX)
    case _ => false
  }

  @RequiredReadAction
  private def create(element: PsiElement, method: PsiMethod): LineMarkerInfo[PsiElement] = {
    val project = element.getProject
    val pair = IconLibraryGroupImageCache.getInstance(project).getImage(method)
    if (pair == null) return null

    val navigationHandler: GutterIconNavigationHandler[PsiElement] =
      (_: ComponentEvent[?], _: PsiElement) => OpenFileDescriptorFactory.getInstance(project).newBuilder(pair.getSecond).build().navigate(true)
    new LineMarkerInfo[PsiElement](
      element,
      element.getTextRange,
      pair.getFirst,
      Pass.LINE_MARKERS,
      null,
      navigationHandler,
      GutterIconRenderer.Alignment.RIGHT
    )
  }

  override def getLanguage: Language = ScalaLanguage.INSTANCE
}
