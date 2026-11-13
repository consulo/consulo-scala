package org.jetbrains.plugins.scalaDirective.lang.completion

import consulo.language.Language
import org.jetbrains.plugins.scalaDirective.ScalaDirectiveLanguage
import consulo.language.editor.completion.lookup.{LookupElement, LookupElementWeigher}
import consulo.language.editor.completion.{CompletionContributor, CompletionService, WeighingContext, CompletionParameters, CompletionProvider, CompletionResultSet, CompletionSorter, CompletionType}
import consulo.application.dumb.DumbAware
import consulo.language.util.ProcessingContext
import org.apache.maven.artifact.versioning.ComparableVersion
import org.jetbrains.plugins.scala.lang.completion.positionFromParameters
import org.jetbrains.plugins.scala.packagesearch.lang.completion.DependencyVersionWeigher
import org.jetbrains.plugins.scala.packagesearch.util.DependencyUtil
import org.jetbrains.plugins.scalaDirective.lang.completion.ScalaDirectiveScalaVersionCompletionContributor.{RealPrefixMatchingWeigher, fetchVersions}
import org.jetbrains.plugins.scalaDirective.lang.completion.lookups.ScalaDirectiveDependencyVersionLookupItem
import org.jetbrains.plugins.scalaDirective.util.ScalaDirectiveValueKind

import scala.jdk.CollectionConverters.IterableHasAsJava

final class ScalaDirectiveScalaVersionCompletionContributor extends CompletionContributor with DumbAware {
  override def getLanguage: Language = ScalaDirectiveLanguage.INSTANCE

  extend(CompletionType.BASIC, ScalaDirectiveScalaVersionPattern, new CompletionProvider {
    override def addCompletions(params: CompletionParameters, processingContext: ProcessingContext, resultSet: CompletionResultSet): Unit = {
      val place = positionFromParameters(using params)
      val (_, valueKind) = ScalaDirectiveValueKind.extract(place.getText)

      val onlyStableVersions = !params.isExtendedCompletion
      val versions = fetchVersions(onlyStableVersions)
      val lookupElements = versions.map(version => ScalaDirectiveDependencyVersionLookupItem(version, valueKind, icon = None))

      val sorter = CompletionSorter.emptySorter()
        .weigh(new RealPrefixMatchingWeigher)
        .weigh(DependencyVersionWeigher)
      val newResultSet = resultSet.withRelevanceSorter(sorter)

      newResultSet.addAllElements(lookupElements.asJava)
      newResultSet.stopHere()
    }
  })
}

object ScalaDirectiveScalaVersionCompletionContributor {
  private def fetchVersions(onlyStable: Boolean): Seq[ComparableVersion] = {
    val scala2 = DependencyUtil.getScala2CompilerVersions(onlyStable)
    val scala3 = DependencyUtil.getScala3CompilerVersions(onlyStable)

    scala2 ++ scala3
  }

  // upstream: com.intellij.codeInsight.completion.impl.RealPrefixMatchingWeigher (impl class in Consulo)
  private[completion] final class RealPrefixMatchingWeigher extends LookupElementWeigher("prefix", false, true) {
    override def weigh(element: LookupElement, context: WeighingContext): Comparable[?] = {
      val matcher = CompletionService.getItemMatcher(element, context)
      var max = Integer.MIN_VALUE
      element.getAllLookupStrings.forEach(s => max = Math.max(max, matcher.matchingDegree(s)))
      Integer.valueOf(-max)
    }
  }
}
