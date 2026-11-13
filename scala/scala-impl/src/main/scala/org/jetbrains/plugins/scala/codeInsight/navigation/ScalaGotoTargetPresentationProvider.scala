package org.jetbrains.plugins.scala.codeInsight.navigation

import com.intellij.java.impl.ide.util.PsiClassOrFunctionalExpressionPresentationProvider
import consulo.application.Application
import consulo.ide.navigation.GotoTargetPresentationProvider
import consulo.language.editor.ui.navigation.PsiTargetPresentationFactory
import consulo.localize.LocalizeValue
import consulo.navigation.TargetPresentation
import consulo.language.psi.PsiElement
import consulo.language.psi.util.PsiTreeUtil
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiPresentationUtils.{extensionMethodContainerText, extensionMethodPresentableText}
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports.ScExportStmt
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition

//noinspection ApiStatus,UnstableApiUsage
final class ScalaGotoTargetPresentationProvider extends GotoTargetPresentationProvider {

  override def getPresentation(element: PsiElement, options: GotoTargetPresentationProvider.Options): TargetPresentation = element match {
    case exportStmt: ScExportStmt =>
      val containingType = Option(PsiTreeUtil.getParentOfType(exportStmt, classOf[ScTypeDefinition]))
      val presentation = containingType.map(typeDefinitionPresentation)
      presentation.orNull
    case function: ScFunction if function.isExtensionMethod =>
      extensionMethodPresentation(function)
    case _ =>
      null
  }

  private def typeDefinitionPresentation(containingType: ScTypeDefinition): TargetPresentation =
    new PsiClassOrFunctionalExpressionPresentationProvider().getPresentation(containingType)

  private def extensionMethodPresentation(function: ScFunction): TargetPresentation = {
    val builder = Application.get().getInstance(classOf[PsiTargetPresentationFactory])
      .presentationBuilder(function)
      .withPresentableText(LocalizeValue.of(extensionMethodPresentableText(function)))
    extensionMethodContainerText(function)
      .fold(builder)(text => builder.withContainerText(LocalizeValue.of(text)))
      .build()
  }
}
