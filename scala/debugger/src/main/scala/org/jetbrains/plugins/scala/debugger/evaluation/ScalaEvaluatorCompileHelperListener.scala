package org.jetbrains.plugins.scala.debugger.evaluation

import com.intellij.java.debugger.impl.{DebuggerManagerListener, DebuggerSession}
import consulo.annotation.component.{ComponentScope, TopicImpl}
import consulo.project.Project
import jakarta.inject.Inject

@TopicImpl(ComponentScope.PROJECT)
final class ScalaEvaluatorCompileHelperListener @Inject()(project: Project) extends DebuggerManagerListener {

  // upstream: also stops the compile server if it is disabled in the settings
  override def sessionDetached(session: DebuggerSession): Unit =
    ScalaEvaluatorCompileHelper.instance(project).clearTempFiles()
}
