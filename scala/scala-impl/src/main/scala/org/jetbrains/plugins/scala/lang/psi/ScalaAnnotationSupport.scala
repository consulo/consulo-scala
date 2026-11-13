package org.jetbrains.plugins.scala.lang.psi

import consulo.util.lang.StringUtil
import com.intellij.java.language.psi.{PsiAnnotationSupport, PsiLiteral}
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.api.base.ScLiteral
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createExpressionFromText

class ScalaAnnotationSupport extends PsiAnnotationSupport {
  override def createLiteralValue(value: String, context: PsiElement): PsiLiteral =
    createExpressionFromText("\"" + StringUtil.escapeStringCharacters(value) + "\"", context)(using context.getManager)
      .asInstanceOf[ScLiteral]
}