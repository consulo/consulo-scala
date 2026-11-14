package org.jetbrains.plugins.scala.codeInsight.hints

import consulo.language.editor.inlay.InlayParameterHintsProvider
import consulo.language.Language
import consulo.localize.LocalizeValue
import com.intellij.java.language.JavaLanguage.{INSTANCE => JavaLanguage}
import org.jetbrains.plugins.scala.api.ScalaLanguage

import java.util
import java.util.Collections

/** This class is used directly rather than via plugin.xml */
private object ScalaInlayParameterHintsProvider extends InlayParameterHintsProvider {
  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def getDefaultBlackList: util.Set[String] = Collections.singleton("scala.*")

  override def getBlackListDependencyLanguage: Language = JavaLanguage

  override def getPreviewFileText: LocalizeValue = LocalizeValue.empty()
}
