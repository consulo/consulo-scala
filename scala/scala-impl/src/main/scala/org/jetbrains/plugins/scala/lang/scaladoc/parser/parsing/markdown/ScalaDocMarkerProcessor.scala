package org.jetbrains.plugins.scala.lang.scaladoc.parser.parsing.markdown

import org.intellij.markdown.flavours.commonmark.CommonMarkMarkerProcessor
import org.intellij.markdown.flavours.gfm.table.GitHubTableMarkerProvider
import org.intellij.markdown.parser.constraints.{CommonMarkdownConstraints, MarkdownConstraints}
import org.intellij.markdown.parser.markerblocks.MarkerBlockProvider
import org.intellij.markdown.parser.markerblocks.providers._
import org.intellij.markdown.parser.{LookaheadText, MarkerProcessor, ProductionHolder}

import java.{util => ju}

class ScalaDocMarkerProcessor(productionHolder: ProductionHolder, constraints: CommonMarkdownConstraints)
  extends CommonMarkMarkerProcessor(productionHolder, constraints) {

  private val markerBlockProviders = ju.List.of(
    new HorizontalRuleProvider,
    new CodeFenceProvider,
    new ScalaDocCodeFenceProvider,
    new ScalaDocTagMarkerBlockProvider,
    new SetextHeaderProvider,
    new BlockQuoteProvider,
    new ListMarkerProvider,
    new AtxHeaderProvider,
    new HtmlBlockProvider,
    new LinkReferenceDefinitionProvider,

    // gfm
    new GitHubTableMarkerProvider,
  )

  // upstream: reflectively delegated to `GFMMarkerProcessor.populateConstraintsTokens`, which is protected (and not
  //           reflectively accessible across Consulo modules). It differs from CommonMark only for `GFMConstraints` with a
  //           checkbox, and the constraints here are always `ScalaDocMarkdownConstraints`, so CommonMark's is the same.
  override def populateConstraintsTokens(pos: LookaheadText#Position, constraints: MarkdownConstraints, productionHolder: ProductionHolder): Unit =
    super.populateConstraintsTokens(pos, constraints, productionHolder)

  override def getMarkerBlockProviders: ju.List[MarkerBlockProvider[MarkerProcessor.StateInfo]] = markerBlockProviders
}