package org.jetbrains.sbt.language.completion

import consulo.language.editor.completion.lookup.{LookupElement, LookupElementWeigher}
import consulo.language.editor.completion.{CompletionService, WeighingContext}

// upstream: com.intellij.codeInsight.completion.impl.RealPrefixMatchingWeigher (impl class in Consulo)
private[completion] final class RealPrefixMatchingWeigher extends LookupElementWeigher("prefix", false, true) {
  override def weigh(element: LookupElement, context: WeighingContext): Comparable[?] = {
    val matcher = CompletionService.getItemMatcher(element, context)
    var max = Integer.MIN_VALUE
    element.getAllLookupStrings.forEach(s => max = Math.max(max, matcher.matchingDegree(s)))
    Integer.valueOf(-max)
  }
}
