package org.jetbrains.plugins.scala.highlighter

import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.rawHighlight.HighlightInfoHolder
import consulo.language.editor.rawHighlight.{HighlightInfo, HighlightInfoType, HighlightInfoTypeImpl, HighlightVisitor}
import consulo.language.editor.annotation.HighlightSeverity
import consulo.codeEditor.HighlighterColors
import consulo.colorScheme.TextAttributesKey
import consulo.application.dumb.DumbAware
import consulo.document.util.TextRange
import consulo.language.psi.{PsiElement, PsiFile}
import org.jetbrains.plugins.scala.extensions.{ObjectExt, PsiElementExt}
import org.jetbrains.plugins.scala.highlighter.ScalaColorsSchemeUtils.NamedArgument
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes.SOFT_KEYWORDS
import org.jetbrains.plugins.scala.api.lang.psi.api.ScFile
import org.jetbrains.plugins.scala.lang.psi.api.base.ScAnnotation
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScAssignment
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScTypeAlias
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameter
import org.jetbrains.plugins.scala.lang.scaladoc.psi.api.ScPsiDocToken

// HighlightVisitor is faster than Annotator in complex code (see SCL-23603)
@ExtensionImpl
class ScalaSyntaxHighlightingVisitor extends HighlightVisitor with DumbAware {
  private var holder: HighlightInfoHolder = scala.compiletime.uninitialized

  override def suitableForFile(file: PsiFile): Boolean =
    file.is[ScFile]

  override def analyze(file: PsiFile, updateWholeFile: Boolean, holder: HighlightInfoHolder, action: Runnable): Boolean = {
    this.holder = holder
    try {
      action.run()
    } finally {
      this.holder = null
    }
    true
  }

  override def visit(element: PsiElement): Unit = element match {
    case NamedArgument(assignment: ScAssignment) =>
      val parameterRefRange = assignment.leftExpression.getNode.getTextRange
      val start = parameterRefRange.getStartOffset
      val end = assignment.assignmentToken.map(_.endOffset).getOrElse(parameterRefRange.getEndOffset)
      holder.add(info(TextRange.create(start, end), ScalaHighlightInfoTypes.NAMED_ARGUMENT))

    case annotation: ScAnnotation =>
      holder.add(info(annotation.getFirstChild, ScalaHighlightInfoTypes.ANNOTATION))
      holder.add(info(annotation.annotationExpr.constructorInvocation.typeElement, ScalaHighlightInfoTypes.ANNOTATION))
      annotation.annotationExpr.getAttributes.foreach { attribute =>
        Option(attribute.nameId).foreach { nameId =>
          holder.add(info(nameId, ScalaHighlightInfoTypes.ANNOTATION_ATTRIBUTE))
        }
      }

    case parameter: ScParameter  =>
      val nameId = parameter.nameId
      //in scala 3 there are anonymous context parameters which don't have name identifier
      if (nameId != null) {
        val attributesKey = ScalaColorsSchemeUtils.parameterHighlightInfoType(parameter)
        holder.add(info(nameId, attributesKey))
      }

    case typeAlias: ScTypeAlias  =>
      holder.add(info(typeAlias.nameId, ScalaHighlightInfoTypes.TYPE_ALIAS))

    case doc: ScPsiDocToken =>
      ScalaSyntaxHighlighter.Attributes.get(doc.tokenType) match {
        case Some(attr) if attr != DefaultHighlighter.DOC_COMMENT =>
          holder.add(info(doc, attr))
        case _ =>
      }

    case e if isSoftKeyword(e) =>
      holder.add(info(e, ScalaHighlightInfoTypes.KEYWORD))

    case e if e.getNode.getElementType == ScalaTokenTypes.tIDENTIFIER =>
      ScalaColorsSchemeUtils
        .findHighlightInfoTypeByParent(e)
        .foreach(a => holder.add(info(e, a)))

    case _ =>
  }

  private def info(e: PsiElement, highlightInfoType: HighlightInfoType): HighlightInfo =
    info(e.getTextRange, highlightInfoType)

  private def info(range: TextRange, highlightInfoType: HighlightInfoType): HighlightInfo =
    HighlightInfo.newHighlightInfo(highlightInfoType)
      .range(range)
      .create()

  private def info(e: PsiElement, attr: TextAttributesKey): HighlightInfo =
    HighlightInfo.newHighlightInfo(ScalaSyntaxHighlightingVisitor.TextAttributesType)
      .range(e)
      .textAttributes(attr)
      .create()

  private def isSoftKeyword(element: PsiElement): Boolean =
    SOFT_KEYWORDS.contains(element.getNode.getElementType)

  override def clone(): HighlightVisitor =
    new ScalaSyntaxHighlightingVisitor()
}

private object ScalaSyntaxHighlightingVisitor {
  // upstream: HighlightInfoType.TEXT_ATTRIBUTES (absent in Consulo)
  private val TextAttributesType: HighlightInfoType =
    new HighlightInfoTypeImpl(HighlightSeverity.TEXT_ATTRIBUTES, HighlighterColors.TEXT)
}
