package org.jetbrains.plugins.scala.debugger.evaluation.evaluator.compiling

import java.nio.file.{Files, Path}

class OutputFileObject(file: Path, val origName: String) {
  def toByteArray: Array[Byte] = Files.readAllBytes(file)
}
