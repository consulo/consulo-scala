package org.jetbrains.plugins.scala.debugger.evaluation

import com.intellij.java.debugger.impl.{DebuggerManagerListener, DebuggerSession}
import consulo.annotation.component.{ComponentScope, TopicImpl}
import consulo.project.Project
import jakarta.inject.Inject

@TopicImpl(ComponentScope.PROJECT)
final class ScalaEvaluatorCacheListener @Inject()(project: Project) extends DebuggerManagerListener {

  override def sessionDetached(session: DebuggerSession): Unit =
    ScalaEvaluatorCache.getInstance(project).clear()
}
