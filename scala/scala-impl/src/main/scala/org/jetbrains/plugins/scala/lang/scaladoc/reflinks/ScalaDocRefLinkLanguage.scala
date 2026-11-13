package org.jetbrains.plugins.scala.lang.scaladoc.reflinks

import consulo.language.{DependentLanguage, Language}
import org.jetbrains.plugins.scala.api.ScalaLanguage

final class ScalaDocRefLinkLanguage
  extends Language(ScalaLanguage.INSTANCE, "ScalaDocRefLink")
    with DependentLanguage

object ScalaDocRefLinkLanguage {
  val INSTANCE: ScalaDocRefLinkLanguage = new ScalaDocRefLinkLanguage
}