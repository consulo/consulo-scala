package org.jetbrains.sbt.language.completion

import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.completion.{CompletionLocation, CompletionWeigher}
import consulo.language.editor.completion.lookup.LookupElement
import org.jetbrains.plugins.scala.lang.completion.lookups.ScalaLookupItem

@ExtensionImpl(id = "scalaSbtDefinitionWeigher", order = "before scalaContainingClassWeigher, before scalaMethodCompletionWeigher")
final class SbtDefinitionWeigher extends CompletionWeigher {

  override def weigh(element: LookupElement,
                     location: CompletionLocation): Comparable[?] = element match {
    case element: ScalaLookupItem
      if element.isSbtLookupItem &&
        element.getLookupString != "???" =>
      if (element.isLocalVariable) 2 else 1
    case _ => 0
  }
}
