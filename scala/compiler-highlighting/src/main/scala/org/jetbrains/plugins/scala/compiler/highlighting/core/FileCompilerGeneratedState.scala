package org.jetbrains.plugins.scala.compiler.highlighting.core

import org.jetbrains.jps.incremental.scala.shared.Client.PosInfo
import org.jetbrains.plugins.scala.shared.util.CompilationId

case class FileCompilerGeneratedState(compilationId: CompilationId,
                                      textStamp: Long,
                                      highlightings: Set[ExternalHighlighting],
                                      types: Map[(PosInfo, PosInfo), String]) {

  def withExtraHighlightings(highlightings: Set[ExternalHighlighting]): FileCompilerGeneratedState =
    this.copy(highlightings = this.highlightings ++ highlightings)

  def withExtraTypes(types: Map[(PosInfo, PosInfo), String]): FileCompilerGeneratedState =
    this.copy(types = this.types ++ types)
}
