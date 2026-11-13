package org.jetbrains.jps.incremental.scala.shared.remote
import java.nio.file.Path

trait PathTranslator {
  def translate(path: Path): String
}
