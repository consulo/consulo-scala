package org.jetbrains.plugins.scala.shared.compiler
import org.jetbrains.plugins.scala.shared.compiler.CompilationUnitId.ModuleId

case class CompilationUnitId(moduleId: ModuleId,
                             testScope: Boolean)

object CompilationUnitId {
  type ModuleId = String
}