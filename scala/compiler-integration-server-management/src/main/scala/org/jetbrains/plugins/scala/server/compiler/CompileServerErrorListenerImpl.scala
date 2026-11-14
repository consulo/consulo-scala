package org.jetbrains.plugins.scala.server.compiler
import consulo.annotation.component.{ComponentScope, TopicImpl}
import consulo.application.ApplicationManager

@TopicImpl(ComponentScope.APPLICATION)
private final class CompileServerErrorListenerImpl extends CompileServerErrorListener {
  override def onError(text: String): Unit = {
    // upstream: activeInTestMode="false"
    if (ApplicationManager.getApplication.isUnitTestMode) return
    CompileServerErrorNotificationService.instance().onError(text)
  }
}
