package org.jetbrains.plugins.scala.codeInsight.template.macros

import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.template.{Expression, ExpressionContext, TextResult}
import com.intellij.java.language.psi.PsiClass
import consulo.language.psi.util.PsiTreeUtil

@ExtensionImpl
final class ScalaQualifiedClassNameMacro extends ScalaMacro {

  override def getNameShort: String = "qualifiedClassName"

  override def getDefaultValue: String = ScalaMacro.DefaultValue

  override def calculateResult(params: Array[Expression], context: ExpressionContext): Result = {
    val parentClass = PsiTreeUtil.getParentOfType(context.getPsiElementAtStartOffset, classOf[PsiClass])
    Option(parentClass).map(_.getQualifiedName).map(new TextResult(_)).orNull
  }
}
