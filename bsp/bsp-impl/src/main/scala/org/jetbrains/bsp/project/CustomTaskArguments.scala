package org.jetbrains.bsp.project

import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.build.BuildReporter

/**
 * Used for passing custom arguments to `BspTask` when running compiler based highlighting.
 * upstream: the reporter is a CompilerEventReporter - [[BspCompilerRunner]] passes its own reporter for the Consulo build
 */
private[jetbrains] case class CustomTaskArguments(@Nls message: String, reporter: BuildReporter)
