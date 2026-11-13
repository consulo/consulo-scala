package org.jetbrains.plugins.scala.scalaMeta

import consulo.application.ApplicationManager

trait ScalaMetaApi
  extends QuasiquoteInferUtilApi
  with ScalaMetaParseExceptionApi

object ScalaMetaApi {
  def instance: ScalaMetaApi = ApplicationManager.getApplication.getInstance(classOf[ScalaMetaApi])
}