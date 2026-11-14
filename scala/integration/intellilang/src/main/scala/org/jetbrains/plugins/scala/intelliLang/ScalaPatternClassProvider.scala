package org.jetbrains.plugins.scala.intelliLang

import consulo.annotation.component.ExtensionImpl
import consulo.language.inject.advanced.pattern.PatternClassProvider
import org.jetbrains.plugins.scala.patterns.ScalaPatterns

@ExtensionImpl
final class ScalaPatternClassProvider extends PatternClassProvider {
  override def getAlias: String = "scala"

  override def getPatternClass: Class[?] = classOf[ScalaPatterns]
}
