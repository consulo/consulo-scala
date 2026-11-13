package org.jetbrains.plugins.scala.codeInspection.dfa

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle

import org.jetbrains.plugins.scala.lang.dfa.analysis.ScalaDfaProblemReporter

@ExtensionImpl
final class DfaUnreachableCodeInspection
  extends DfaInspectionBase(ScalaDfaProblemReporter.reportingUnreachableCode) {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("unreachable.code.dfa.name"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.general"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def isEnabledByDefault: Boolean = false

  override def getLanguage: Language = ScalaLanguage.INSTANCE
}
