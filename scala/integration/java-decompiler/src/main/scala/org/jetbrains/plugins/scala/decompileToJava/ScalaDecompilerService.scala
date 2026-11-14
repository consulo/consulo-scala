package org.jetbrains.plugins.scala
package decompileToJava

import consulo.application.ApplicationManager
import org.jetbrains.plugins.scala.api.lang.psi.api.ScFile

import scala.util.Try

trait ScalaDecompilerService {
  def decompile(sourceFile: ScFile): Try[String]
}

object ScalaDecompilerService {
  def apply(): ScalaDecompilerService = ApplicationManager.getApplication.getInstance(classOf[ScalaDecompilerService])
}
