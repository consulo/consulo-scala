package org.jetbrains.idea.devkit.scala

import com.intellij.java.language.codeInsight.AnnotationUtil
import com.intellij.java.language.psi.PsiClass
import consulo.annotation.access.RequiredReadAction
import consulo.annotation.component.ExtensionImpl
import consulo.codeEditor.markup.GutterIconRenderer
import consulo.devkit.inspections.valhalla.ValhallaClasses
import consulo.devkit.util.PluginModuleUtil
import consulo.language.Language
import consulo.language.editor.Pass
import consulo.language.editor.gutter.{GutterIconNavigationHandler, LineMarkerInfo, LineMarkerProvider}
import consulo.language.psi.util.PsiNavigateUtil
import consulo.language.psi.{PsiElement, PsiFile}
import consulo.platform.base.icon.PlatformIconGroup
import consulo.util.lang.StringUtil
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition

import consulo.ui.event.ComponentEvent
import scala.jdk.CollectionConverters.*

/**
 * Gutter icon on `@ExtensionImpl`/`@ServiceImpl`/`@ActionImpl`/`@TopicImpl` Scala classes navigating to the API
 * annotation (`@ExtensionAPI`, ...) of the implemented type, like consulo-devkit's ImplToAPILineMarkerProvider for Java.
 */
@ExtensionImpl
final class ScalaImplToAPILineMarkerProvider extends LineMarkerProvider {
  import ScalaImplToAPILineMarkerProvider.*

  @RequiredReadAction
  override def isAvailable(file: PsiFile): Boolean = PluginModuleUtil.isConsuloOrPluginProject(file)

  @RequiredReadAction
  override def getLineMarkerInfo(element: PsiElement): LineMarkerInfo[?] = element.getParent match {
    case definition: ScTypeDefinition if definition.nameId == element =>
      findAPIElement(definition) match {
        case Some((_, apiAnnotation)) =>
          val navigationText = "Navigate to @" + StringUtil.getShortName(apiAnnotation)
          val tooltip: java.util.function.Function[PsiElement, String] = _ => navigationText
          val navigationHandler: GutterIconNavigationHandler[PsiElement] =
            (_: ComponentEvent[?], _: PsiElement) => findAPIElement(definition).foreach { case (target, _) => PsiNavigateUtil.navigate(target) }
          new LineMarkerInfo[PsiElement](
            element,
            element.getTextRange,
            PlatformIconGroup.nodesPlugin(),
            Pass.LINE_MARKERS,
            tooltip,
            navigationHandler,
            GutterIconRenderer.Alignment.RIGHT
          )
        case None => null
      }
    case _ => null
  }

  override def getLanguage: Language = ScalaLanguage.INSTANCE
}

object ScalaImplToAPILineMarkerProvider {
  private def findAPIElement(psiClass: PsiClass): Option[(PsiElement, String)] =
    ValhallaClasses.API_TO_IMPL.asScala.iterator
      .filter(apiToImpl => AnnotationUtil.isAnnotated(psiClass, apiToImpl.getSecond, 0))
      .flatMap { apiToImpl =>
        Option(AnnotationUtil.findAnnotationInHierarchy(psiClass, java.util.Set.of(apiToImpl.getFirst)))
          .map(annotation => (annotation: PsiElement) -> apiToImpl.getFirst)
      }
      .nextOption()
}
