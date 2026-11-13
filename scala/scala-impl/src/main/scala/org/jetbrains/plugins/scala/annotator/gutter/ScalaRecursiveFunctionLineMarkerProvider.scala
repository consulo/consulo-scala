package org.jetbrains.plugins.scala.annotator.gutter

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.Pass
import consulo.ui.image.Image
import consulo.language.editor.gutter.{LineMarkerInfo, LineMarkerProvider}
import consulo.codeEditor.markup.GutterIconRenderer.Alignment
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.incremental.Highlighting._
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.icons.Icons
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScFunctionDefinition, ScFunctionDefinitionExt}


@ExtensionImpl
final class ScalaRecursiveFunctionLineMarkerProvider extends LineMarkerProvider {
  override def getLanguage: Language = ScalaLanguage.INSTANCE


  import ScalaRecursiveFunctionLineMarkerProvider._

  override def getLineMarkerInfo(element: PsiElement): LineMarkerInfo[? <: PsiElement] = {
    if (!GutterUtil.RecursionOption.isEnabled) {
      return null
    }

    val file = element.getContainingFile
    val project = if (file != null) file.getProject else element.getProject // Avoid tree walk-up

    if (!element.isVisible(project, file)) return null

    element.getParent match {
      case function: ScFunctionDefinition if element.getNode.getElementType == ScalaTokenTypes.tIDENTIFIER =>
        val references = function.recursiveReferencesGrouped
        if (references.tailRecursionOnly) {
          createLineMarkerInfo(
            Icons.TAIL_RECURSION,
            ScalaBundle.message("method.is.tail.recursive", _),
            function.nameId
          )
        }
        else if (!references.noRecursion) {
          createLineMarkerInfo(
            Icons.RECURSION,
            ScalaBundle.message("method.is.recursive", _),
            function.nameId
          )
        }
        else null
      case _ => null
    }
  }
}

object ScalaRecursiveFunctionLineMarkerProvider {
  private def createLineMarkerInfo(icon: Image, psiElemToTooltip: String => String, element: PsiElement): LineMarkerInfo[PsiElement] =
    new LineMarkerInfo(
      element,
      element.getTextRange,
      icon,
      Pass.LINE_MARKERS,
      (e: PsiElement) => psiElemToTooltip(e.getText),
      null,
      Alignment.LEFT
    )
}
