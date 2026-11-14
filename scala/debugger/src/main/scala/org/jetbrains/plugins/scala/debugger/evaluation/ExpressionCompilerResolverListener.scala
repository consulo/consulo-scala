package org.jetbrains.plugins.scala.debugger.evaluation

import com.intellij.java.debugger.impl.{DebuggerManagerListener, DebuggerSession}
import consulo.annotation.component.{ComponentScope, TopicImpl}
import consulo.application.progress.{ProgressIndicator, Task}
import consulo.project.Project
import consulo.util.dataholder.Key
import jakarta.inject.Inject
import org.jetbrains.plugins.scala.DependencyManagerBase.RichStr
import org.jetbrains.plugins.scala.debugger.DebuggerBundle
import org.jetbrains.plugins.scala.project.{ModuleExt, ProjectExt}
import org.jetbrains.plugins.scala.api.project.ScalaLanguageLevel
import org.jetbrains.plugins.scala.{DependencyManagerBase, ScalaVersion}

import java.nio.file.Path
import scala.util.control.NonFatal

@TopicImpl(ComponentScope.PROJECT)
private final class ExpressionCompilerResolverListener @Inject()(project: Project) extends DebuggerManagerListener {

  import ExpressionCompilerResolverListener._

  override def sessionCreated(session: DebuggerSession): Unit = {
    if (project.isDisposed) return

    new Task.Backgroundable(project, DebuggerBundle.message("resolving.expression.compiler"), true) {
      override def run(indicator: ProgressIndicator): Unit = {
        if (project.isDisposed) return

        val expressionCompilers = try {
          project
            .modulesWithScala
            .flatMap(_.scalaMinorVersion)
            .filter(_.isScala3)
            .flatMap { v =>
              if (v.languageLevel >= ScalaLanguageLevel.Scala_3_7)
                Some(v -> ExpressionCompilerType.BuiltIn)
              else
                resolveExpressionCompilerJar(v, indicator).map(jar => v -> ExpressionCompilerType.ResolvedJar(jar))
            }.toMap
        } catch {
          case NonFatal(_) => Map.empty[ScalaVersion, ExpressionCompilerType]
        }

        project.putUserData(ExpressionCompilers, expressionCompilers)
      }
    }.queue()
  }

  // upstream: transfers the jar to the eel of the project, the expression is compiled in the IDE process here
  private def resolveExpressionCompilerJar(scalaVersion: ScalaVersion, indicator: ProgressIndicator): Option[Path] = {
    val dep = "ch.epfl.scala" % s"scala-expression-compiler_${scalaVersion.minor}" % ScalaExpressionCompilerVersion
    val manager = new DependencyManagerBase {
      override protected def progressIndicator: Option[ProgressIndicator] = Some(indicator)
    }
    manager.resolveSafe(dep).toOption.flatMap(_.headOption).map(_.file)
  }
}

private object ExpressionCompilerResolverListener {

  sealed trait ExpressionCompilerType
  object ExpressionCompilerType {
    /**
     * Scala 3.7 ships the debugger expression compiler as part of the scala3-compiler.jar. We do not need to manually
     * resolve a separate expression compiler jar.
     */
    case object BuiltIn extends ExpressionCompilerType

    /**
     * Scala 3.0 to Scala 3.6 use a separate jar which hosts the debugger expression compiler which needs to be
     * manually resolved.
     */
    case class ResolvedJar(path: Path) extends ExpressionCompilerType
  }

  final val ExpressionCompilers: Key[Map[ScalaVersion, ExpressionCompilerType]] = Key.create("scala_debugger_expression_compilers")

  final val ScalaExpressionCompilerVersion = "4.2.13"
}
