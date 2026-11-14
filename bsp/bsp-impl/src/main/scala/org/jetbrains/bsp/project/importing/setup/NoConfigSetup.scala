package org.jetbrains.bsp.project.importing.setup
import consulo.application.progress.ProgressIndicator
import org.jetbrains.plugins.scala.build.{BuildMessages, BuildReporter}

import scala.util.{Success, Try}

object NoConfigSetup extends BspConfigSetup {
  override def run(indicator: ProgressIndicator)(implicit reporter: BuildReporter): Try[BuildMessages] =
    Success(BuildMessages.empty)
}
