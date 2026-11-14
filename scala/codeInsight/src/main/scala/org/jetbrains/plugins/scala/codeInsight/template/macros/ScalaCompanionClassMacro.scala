package org.jetbrains.plugins.scala.codeInsight.template.macros

import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.template.{Expression, ExpressionContext, Result}
import com.intellij.codeInsight.template.PsiElementResult
import com.intellij.java.language.psi.PsiClass
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScClass, ScObject}
import org.jetbrains.plugins.scala.extensions.PsiElementExt
import org.jetbrains.plugins.scala.extensions.OptionExt

@ExtensionImpl
class ScalaCompanionClassMacro extends ScalaMacro {

  override def getNameShort: String = "companionClass"

  override def calculateResult(params: Array[Expression], context: ExpressionContext): Result = {
    val clazz = ScalaCompanionClassMacro.companionClass(context.getPsiElementAtStartOffset)
    clazz.map(new PsiElementResult(_)).orNull
  }
}

object ScalaCompanionClassMacro {

  private[macros] def companionClass(element: PsiElement): Option[ScClass] = {
    element
      .parentOfType(classOf[PsiClass])
      .map {
        case scalaObject: ScObject => scalaObject.fakeCompanionClassOrCompanionClass
        case other                 => other
      }
      .filterByType[ScClass]
  }
}
