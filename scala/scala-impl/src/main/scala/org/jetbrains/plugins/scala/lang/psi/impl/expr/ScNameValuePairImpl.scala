package org.jetbrains.plugins.scala.lang.psi.impl.expr

import consulo.language.ast.ASTNode
import com.intellij.java.language.psi.PsiAnnotationMemberValue
import org.jetbrains.plugins.scala.extensions.ifReadAllowed
import org.jetbrains.plugins.scala.lang.psi.api.base.ScLiteral
import org.jetbrains.plugins.scala.lang.psi.api.expr._
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementImpl

class ScNameValuePairImpl(node: ASTNode) extends ScalaPsiElementImpl(node) with ScNameValuePair {
  override def toString: String = "NameValuePair: " + ifReadAllowed(name)("")

  override def setValue(newValue: PsiAnnotationMemberValue): PsiAnnotationMemberValue = newValue

  override def getValue: PsiAnnotationMemberValue = null

  override def getLiteral: Option[ScLiteral] = findChild[ScLiteral]

  override def getLiteralValue: String = {
    getLiteral match {
      case Some(literal) =>
        val value = literal.getValue
        if (value != null) value.toString
        else null
      case _ => null
    }
  }
}