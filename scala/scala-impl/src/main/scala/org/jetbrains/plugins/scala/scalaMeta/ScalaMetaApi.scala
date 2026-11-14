package org.jetbrains.plugins.scala.scalaMeta

import consulo.annotation.component.{ComponentScope, ServiceAPI}
import consulo.application.ApplicationManager

@ServiceAPI(ComponentScope.APPLICATION)
trait ScalaMetaApi
  extends QuasiquoteInferUtilApi
  with ScalaMetaParseExceptionApi

object ScalaMetaApi {
  def instance: ScalaMetaApi = ApplicationManager.getApplication.getInstance(classOf[ScalaMetaApi])
}