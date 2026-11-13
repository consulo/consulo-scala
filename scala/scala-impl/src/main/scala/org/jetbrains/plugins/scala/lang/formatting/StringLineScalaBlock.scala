package org.jetbrains.plugins.scala.lang.formatting

import consulo.language.codeStyle.{Alignment, Block, ChildAttributes, Indent, Spacing, Wrap}
import consulo.language.ast.ASTNode
import consulo.document.util.TextRange
import consulo.language.codeStyle.CodeStyleSettings

import java.util

private class StringLineScalaBlock(
  myTextRange: TextRange,
  mainNode: ASTNode,
  myAlignment: Alignment,
  myIndent: Indent,
  myWrap: Wrap,
  mySettings: CodeStyleSettings
) extends ScalaBlock(mainNode, null, myAlignment, myIndent, myWrap, mySettings) {

  override def getTextRange: TextRange = myTextRange
  override def isLeaf = true
  override def isLeaf(node: ASTNode): Boolean = true
  override def getChildAttributes(newChildIndex: Int): ChildAttributes = new ChildAttributes(Indent.getNoneIndent, null)
  override def getSpacing(child1: Block, child2: Block): Spacing = Spacing.getReadOnlySpacing
  override def getSubBlocks: util.List[Block] = {
    if (subBlocks == null) {
      subBlocks = new util.ArrayList[Block]()
    }
    subBlocks
  }
}