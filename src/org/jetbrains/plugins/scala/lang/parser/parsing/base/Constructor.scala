package org.jetbrains.plugins.scala
package lang
package parser
package parsing
package base

import com.intellij.lang.PsiBuilder
import expressions.ArgumentExprs
import lexer.ScalaTokenTypes
import types.{SimpleType, AnnotType}
import builder.ScalaPsiBuilder

/**
 * @author AlexanderPodkhalyuzin
 */

/*
 * Constr ::= AnnotType {ArgumentExprs}
 */

object Constructor {
  def parse(builder: ScalaPsiBuilder): Boolean = parse(builder, false)
  def parse(builder: ScalaPsiBuilder, isAnnotation: Boolean): Boolean = {
    val constrMarker = builder.mark
    if ((!isAnnotation && !AnnotType.parse(builder)) || (isAnnotation && !SimpleType.parse(builder))) {
      constrMarker.drop
      return false
    }
    if (builder.getTokenType == ScalaTokenTypes.tLPARENTHESIS) {
      ArgumentExprs parse builder
      while (builder.getTokenType == ScalaTokenTypes.tLPARENTHESIS && !isAnnotation) {
        ArgumentExprs parse builder
      }
    }
    constrMarker.done(ScalaElementTypes.CONSTRUCTOR)
    return true
  }
}