package org.jetbrains.plugins.scala
package debugger.evaluation

import com.intellij.java.debugger.engine.evaluation.{EvaluateException, EvaluateExceptionUtil}
import org.jetbrains.annotations.Nls

import scala.annotation.tailrec

object EvaluationException {
  def apply(@Nls message: String): EvaluateException = EvaluateExceptionUtil.createEvaluateException(message)
  def apply(thr: Throwable): EvaluateException = createEvaluateException(null, thr)
  def apply(@Nls message: String, thr: Throwable): EvaluateException = createEvaluateException(message, thr)

  // EvaluateExceptionUtil.createEvaluateException lose information about original exception
  // if it is an EvaluateException with null cause
  private def createEvaluateException(@Nls msg: String, thr: Throwable): EvaluateException = {
    val message = EvaluateExceptionUtil.createEvaluateException(msg, thr).getMessage
    // upstream: AbsentInformationException is wrapped into AbsentInformationEvaluateException, missing in consulo-java
    new EvaluateException(message, getRootCause(thr))
  }

  @tailrec
  private def getRootCause(thr: Throwable): Throwable = thr.getCause match {
    case e: Throwable if e == thr => thr
    case e: Throwable             => getRootCause(e)
    case null                     => thr
  }
}
