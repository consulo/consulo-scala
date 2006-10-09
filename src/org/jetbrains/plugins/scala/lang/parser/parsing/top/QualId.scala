package org.jetbrains.plugins.scala.lang.parser.parsing.top
import com.intellij.lang.PsiBuilder, org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.parser.ScalaElementTypes
/**
 * User: Dmitry.Krasilschikov
 * Date: 03.10.2006
 * Time: 18:15:12
 */

class QualId {
  def parse(builder : PsiBuilder, marker : PsiBuilder.Marker ) : Unit = {

    //builder.advanceLexer   // Ate QualID identifier

    builder.getTokenType match {
      case ScalaTokenTypes.tDOT => {

        val preMarker = marker.precede()
        marker.done(ScalaElementTypes.QUALID)

        val dotMarker = builder.mark();
        builder.advanceLexer // Ate dot
        dotMarker.done(ScalaElementTypes.DOT)

        builder.getTokenType match {
          case ScalaTokenTypes.tIDENTIFIER => {

            val idMarker = builder.mark()
            builder.advanceLexer
            idMarker.done(ScalaElementTypes.IDENTIFIER)

            (new QualId).parse(builder,preMarker)
          }
          case _ => builder.error("Wrong package name declaration")
        }
      }

      case ScalaTokenTypes.tWHITE_SPACE_LINE_TERMINATE => { //End of package
        marker.done(ScalaElementTypes.QUALID)
        builder.advanceLexer
      }
      case ScalaTokenTypes.tSEMICOLON => { //End of package
        marker.done(ScalaElementTypes.QUALID)
        val semMarker = builder.mark()
        builder.advanceLexer
        semMarker.done(ScalaElementTypes.SEMICOLON)
      }
      case _ => builder.error("Wrong package name declaration")
    }


  }

}
