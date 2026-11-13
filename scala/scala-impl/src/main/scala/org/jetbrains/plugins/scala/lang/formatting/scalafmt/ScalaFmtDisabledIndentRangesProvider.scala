package org.jetbrains.plugins.scala.lang.formatting.scalafmt

import consulo.document.util.TextRange
import consulo.language.psi.PsiElement
import consulo.language.codeStyle.DisabledIndentRangesProvider
import org.jetbrains.plugins.scala.lang.formatting.settings.ScalaCodeStyleSettings

import java.util

class ScalaFmtDisabledIndentRangesProvider extends DisabledIndentRangesProvider {

  override def getDisabledIndentRanges(element: PsiElement): util.Collection[TextRange] = {
    val project = element.getProject
    //Do not try to fix indents after formatting - ScalaFmtPreformatProcessor delegates all the work to scalafmt
    import scala.jdk.CollectionConverters._
    if (ScalaCodeStyleSettings.getInstance(project).USE_SCALAFMT_FORMATTER()) Seq(element.getTextRange).asJava
    else Seq().asJava
  }
}
