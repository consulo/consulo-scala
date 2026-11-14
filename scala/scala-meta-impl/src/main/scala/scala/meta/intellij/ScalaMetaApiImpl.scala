package scala.meta.intellij

import consulo.annotation.component.ServiceImpl
import jakarta.inject.Singleton
import org.jetbrains.plugins.scala.scalaMeta.ScalaMetaApi

@Singleton
@ServiceImpl
final class ScalaMetaApiImpl
  extends ScalaMetaApi
  with QuasiquoteInferUtilApiImpl
  with ScalaMetaParseExceptionApiImpl
