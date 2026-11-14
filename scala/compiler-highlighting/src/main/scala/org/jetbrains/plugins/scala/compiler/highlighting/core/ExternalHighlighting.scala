package org.jetbrains.plugins.scala.compiler.highlighting.core

import consulo.language.editor.rawHighlight.HighlightInfoType
import org.jetbrains.jps.incremental.scala.shared.Client.PosInfo
import org.jetbrains.plugins.scala.compiler.diagnostics.Action
import org.jetbrains.plugins.scala.compiler.highlighting.core.ExternalHighlighting.RangeInfo

/**
 * All information that needed for highlighting.
 *
 * Note: potentially this class can be a sealed ADT.
 *
 * @param rangeInfo can be None if a message was produce for a file, but no location was specified (for some reason)
 */
final case class ExternalHighlighting(highlightType: HighlightInfoType,
                                      message: String,
                                      rangeInfo: Option[RangeInfo],
                                      diagnostics: List[Action])

object ExternalHighlighting {
  sealed trait RangeInfo

  object RangeInfo {
    final case class Range(problemStart: PosInfo, problemEnd: PosInfo, debugTag: String) extends RangeInfo
    final case class Pointer(pointer: PosInfo) extends RangeInfo
  }
}
