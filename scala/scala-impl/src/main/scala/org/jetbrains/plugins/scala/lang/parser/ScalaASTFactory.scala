package org.jetbrains.plugins.scala.lang.parser

import consulo.annotation.component.ExtensionImpl
import consulo.language.impl.ast.{ASTLeafFactory, LeafElement}
import consulo.language.ast.IElementType
import consulo.language.version.LanguageVersion
import org.jetbrains.plugins.scalaDirective.psi.impl.ScDirectiveTokenImpl
import org.jetbrains.plugins.scala.lang.scaladoc.lexer.ScalaDocElementType
import org.jetbrains.plugins.scala.lang.scaladoc.psi.impl.ScPsiDocTokenImpl
import org.jetbrains.plugins.scalaDirective.lang.lexer.ScalaDirectiveElementType

/**
 * upstream: extends DefaultASTFactoryImpl (registered per language); in Consulo leaf factories are
 * selected by element type, other leaves are created by the default factory
 *
 * @see [[com.intellij.psi.impl.source.tree.JavaASTFactory]]
 */
@ExtensionImpl
final class ScalaASTFactory extends ASTLeafFactory {

  override def createLeaf(typ: IElementType, languageVersion: LanguageVersion, text: CharSequence): LeafElement =
    typ match {
      case cliElement: ScalaDirectiveElementType =>
        new ScDirectiveTokenImpl(cliElement, text)
      case scDoc: ScalaDocElementType => new ScPsiDocTokenImpl(scDoc, text)
      case _ => throw new IllegalArgumentException(s"Unexpected element type: $typ")
    }

  override def test(typ: IElementType): Boolean =
    typ.isInstanceOf[ScalaDirectiveElementType] || typ.isInstanceOf[ScalaDocElementType]
}
