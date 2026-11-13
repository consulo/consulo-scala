package org.jetbrains.plugins.scala.lang.completion.postfix

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.postfixTemplate.{PostfixTemplate, PostfixTemplateProvider}
import consulo.codeEditor.Editor
import consulo.language.psi.PsiFile

import java.{util => ju}
import scala.jdk.CollectionConverters._

@ExtensionImpl
final class ScalaPostfixTemplateProvider extends PostfixTemplateProvider {
  override def getLanguage: Language = ScalaLanguage.INSTANCE


  override def getTemplates: ju.Set[PostfixTemplate] = {
    ScalaPostfixTemplateProvider.Templates.asJava
  }

  override def isTerminalSymbol(currentChar: Char): Boolean = currentChar match {
    case '.' | '!' => true
    case _ => false
  }

  override def preExpand(file: PsiFile, editor: Editor): Unit = {}

  override def preCheck(copyFile: PsiFile, realEditor: Editor, currentOffset: Int): PsiFile = copyFile

  override def afterExpand(file: PsiFile, editor: Editor): Unit = {}
}

object ScalaPostfixTemplateProvider {

  import templates._

  private[postfix] val Templates = Set[PostfixTemplate](
    new ScalaTryPostfixTemplate,
    new ScalaAssertPostfixTemplate,
    new ScalaCastPostfixTemplate,
    new ScalaElseExpressionPostfixTemplate,
    new ScalaIfExpressionPostfixTemplate,
    new ScalaMatchPostfixTemplate,
    new ScalaForEachPostfixTemplate,
    new ScalaIntroduceFieldPostfixTemplate,
    new ScalaIntroduceVariablePostfixTemplate,
    new ScalaNotPostfixTemplate,
    new ScalaNotPostfixTemplate("!", false),
    new ScalaParenthesizedExpressionPostfixTemplate,
    new ScalaReturnPostfixTemplate,
    new ScalaPrintlnPostfixTemplate,
    new ScalaPrintlnPostfixTemplate("prtln"),
    new ScalaThrowExceptionPostfixTemplate,
    new ScalaWhilePostfixTemplate,
    new ScalaDoWhilePostfixTemplate,
    new ScalaIsNullPostfixTemplate,
    new ScalaNotNullPostfixTemplate,
    new ScalaNotNullPostfixTemplate("nn"),
    new ScalaOptionPostfixTemplate,
    new ScalaSeqPostfixTemplate,
    new ScalaListPostfixTemplate,
    new ScalaExhaustiveMatchPostfixTemplate
  )
}
