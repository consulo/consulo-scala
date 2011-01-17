package org.jetbrains.plugins.scala
package lang
package parser
package parsing
package types

import com.intellij.lang.PsiBuilder, org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.ScalaBundle
import builder.ScalaPsiBuilder

/**
* @author Alexander Podkhalyuzin
* Date: 28.02.2008
*/

object RefineStatSeq {
  def parse(builder: ScalaPsiBuilder) {
    while (true) {
      builder.getTokenType match {
        //end of parsing when find } or builder.eof
        case ScalaTokenTypes.tRBRACE | null => return
        case ScalaTokenTypes.tSEMICOLON => builder.advanceLexer //not interesting case
        //otherwise parse TopStat
        case _ => {
          if (!RefineStat.parse(builder)) {
            builder error ScalaBundle.message("wrong.top.statment.declaration")
            builder.advanceLexer
          }
          else {
            builder.getTokenType match {
              case ScalaTokenTypes.tSEMICOLON => builder.advanceLexer //it is good
              case null | ScalaTokenTypes.tRBRACE => return
              case _ if !builder.newlineBeforeCurrentToken => builder error ScalaBundle.message("semi.expected")
              case _ =>
            }
          }
        }
      }
    }
  }
}