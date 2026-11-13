package org.jetbrains.plugins.scala.lang.scaladoc.parser.parsing.markdown

import consulo.language.ast.{ASTNode, IElementType, ILazyParseableElementType}
import consulo.language.impl.ast.{ASTFactory, CompositeElement, TreeElement}
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.parser.{CancellationToken, MarkdownParser, MarkdownProductionSink}

import java.{util => ju}

/**
 * Standalone (file-less) Markdown trees on the Consulo markdown engine.
 *
 * Upstream used the trees of the Kotlin `org.jetbrains:markdown` library (`MarkdownParser.parse`, `CompositeASTNode`).
 * The Consulo engine produces a stream of productions instead, which are turned into a [[consulo.language.ast.ASTNode]]
 * tree here - the counterpart of consulo-markdown's `TreeBuildingSink`, which lives in an impl module.
 */
object ScalaDocMarkdownTree {

  def parse(flavour: MarkdownFlavourDescriptor, text: CharSequence, cancellationToken: CancellationToken): ASTNode = {
    val parser = new MarkdownParser(flavour, true, cancellationToken)
    val sink = new TreeBuildingSink(text)
    parser.walkBlocks(parser.parseBlocks(MarkdownElementTypes.MARKDOWN_FILE, text), text, true, sink)
    sink.root
  }

  /**
   * A new composite node with copies of `children` - the counterpart of upstream's `CompositeASTNode(type, children)`.
   *
   * Offsets of Consulo AST nodes are derived from the position in the tree, so to keep the offsets of the original
   * tree (which the HTML generator uses to cut the text) `children` must be consecutive siblings starting at offset 0.
   */
  def composite(elementType: IElementType, children: ju.List[ASTNode]): ASTNode = {
    assert(children.isEmpty || children.get(0).getStartOffset == 0, "children must start at offset 0")
    val node = ASTFactory.composite(elementType)
    children.forEach { child =>
      node.rawAddChildrenWithoutNotifications(child.asInstanceOf[TreeElement].clone().asInstanceOf[TreeElement])
    }
    node
  }

  /**
   * Lazy parseable types are created already parsed (no chameleon text), so their children are the ones produced here
   * with the given flavour.
   */
  private final class TreeBuildingSink(text: CharSequence) extends MarkdownProductionSink {
    private val stack = new ju.ArrayDeque[CompositeElement]()
    private var rootElement: CompositeElement = null

    def root: ASTNode = {
      if (rootElement == null || !stack.isEmpty) throw new IllegalStateException("tree is not complete")
      rootElement
    }

    override def open(elementType: IElementType, startOffset: Int): Unit = {
      val element = elementType match {
        case lazyType: ILazyParseableElementType => ASTFactory.`lazy`(lazyType, null)
        case _ => ASTFactory.composite(elementType)
      }
      val parent = stack.peek()
      if (parent != null) parent.rawAddChildrenWithoutNotifications(element)
      else rootElement = element
      stack.push(element)
    }

    override def close(elementType: IElementType, endOffset: Int): Unit =
      stack.pop()

    override def leaf(elementType: IElementType, startOffset: Int, endOffset: Int): Unit = {
      if (startOffset != endOffset) {
        val parent = stack.peek()
        if (parent == null) throw new IllegalStateException("leaf outside of the root")
        parent.rawAddChildrenWithoutNotifications(ASTFactory.leaf(elementType, text.subSequence(startOffset, endOffset)))
      }
    }
  }
}
