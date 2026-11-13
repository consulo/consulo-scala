package org.jetbrains.plugins.scala.annotator

import consulo.language.editor.intention.IntentionAction
import consulo.language.editor.rawHighlight.HighlightDisplayKey
import consulo.language.editor.inspection.{LocalQuickFix, ProblemDescriptor, ProblemHighlightType}
import consulo.language.ast.ASTNode
import consulo.language.editor.annotation.{HighlightSeverity, ProblemGroup}
import consulo.colorScheme.TextAttributesKey
import consulo.codeEditor.markup.GutterIconRenderer
import consulo.colorScheme.TextAttributes
import consulo.document.util.TextRange
import consulo.language.psi.PsiElement
import org.jetbrains.annotations.{Nls, Nullable}

/**
 * To be used in tests and annotator-based inspection, where no instance of AnnotationHolder is available
 */
abstract class DummyScalaAnnotationBuilder(severity: HighlightSeverity, @Nullable @Nls message: String)
  extends ScalaAnnotationBuilder {

  private var rangeTransformer: TextRange => TextRange = identity
  private var range: TextRange = scala.compiletime.uninitialized
  private var enforcedAttributes: TextAttributesKey = scala.compiletime.uninitialized
  private val fixes = Seq.newBuilder[IntentionAction]

  override def setRangeTransformer(transformer: TextRange => TextRange): this.type = {
    rangeTransformer = transformer
    this
  }

  override def range(range: TextRange): this.type = {
    this.range = range
    this
  }

  override def range(element: ASTNode): this.type = {
    this.range = element.getTextRange
    this
  }

  override def range(element: PsiElement): this.type = {
    this.range = element.getTextRange
    this
  }

  override def textAttributes(enforcedAttributes: TextAttributesKey): this.type = {
    this.enforcedAttributes = enforcedAttributes
    this
  }

  def onCreate(severity: HighlightSeverity, @Nls message: String, range: TextRange,
               enforcedAttributes: TextAttributesKey, fixes: Seq[IntentionAction]): Unit

  override def create(): Unit = {
    onCreate(severity, message, rangeTransformer(range), enforcedAttributes, fixes.result())
    fixes.clear()
  }

  override def highlightType(highlightType: ProblemHighlightType): this.type = this

  override def afterEndOfLine: this.type = this

  override def fileLevel: this.type = this

  override def gutterIconRenderer(gutterIconRenderer: GutterIconRenderer): this.type = this

  override def problemGroup(problemGroup: ProblemGroup): this.type = this

  override def enforcedTextAttributes(enforcedAttributes: TextAttributes): this.type = this

  override def tooltip(tooltip: String): this.type = this

  override def needsUpdateOnTyping: this.type = this

  override def needsUpdateOnTyping(value: Boolean): this.type = this

  override def withFix(fix: IntentionAction): this.type = {
    fixes += fix
    this
  }

  override def newFix(fix: IntentionAction): ScalaAnnotationBuilder.FixBuilder = new DummyFixBuilder(fix)

  override def newLocalQuickFix(fix: LocalQuickFix, problemDescriptor: ProblemDescriptor): ScalaAnnotationBuilder.FixBuilder =
    new DummyFixBuilder

  private final class DummyFixBuilder(@Nullable private val fix: IntentionAction) extends ScalaAnnotationBuilder.FixBuilder {
    def this() = this(null) // TODO: track local quick-fixes as well

    override def range(range: TextRange): ScalaAnnotationBuilder.FixBuilder = this

    override def key(key: HighlightDisplayKey): ScalaAnnotationBuilder.FixBuilder = this

    override def batch: ScalaAnnotationBuilder.FixBuilder = this

    override def universal: ScalaAnnotationBuilder.FixBuilder = this

    override def registerFix: DummyScalaAnnotationBuilder.this.type = {
      if (fix != null) {
        DummyScalaAnnotationBuilder.this.fixes += fix
      }

      DummyScalaAnnotationBuilder.this
    }
  }
}
