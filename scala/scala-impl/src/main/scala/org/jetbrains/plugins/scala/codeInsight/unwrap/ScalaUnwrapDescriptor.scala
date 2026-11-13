package org.jetbrains.plugins.scala.codeInsight.unwrap

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.refactoring.unwrap.{UnwrapDescriptorBase, Unwrapper}

@ExtensionImpl
class ScalaUnwrapDescriptor extends UnwrapDescriptorBase {
  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def createUnwrappers(): Array[Unwrapper] = Array (
    new ScalaInfixUnwrapper,
    new ScalaMethodCallArgUnwrapper,
    new ScalaTupleUnwrapper,
    new ScalaWhileUnwrapper,
    new ScalaMatchUnwrapper,
    new ScalaCaseClauseRemover,
    new ScalaTryOrFinallyUnwrapper,
    new ScalaTryWithFinallyUnwrapper,
    new ScalaCatchOrFinallyRemover,
    new ScalaForStmtUnwrapper,
    new ScalaIfUnwrapper,
    new ScalaElseUnwrapper,
    new ScalaElseRemover,
    new ScalaBracesUnwrapper,
    new ScalaInterpolatedStringUnwrapper
  )
}
