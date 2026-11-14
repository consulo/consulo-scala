package org.jetbrains.plugins.scala.debugger.ui

import com.intellij.java.debugger.engine.evaluation.EvaluateException
import com.intellij.java.debugger.localize.JavaDebuggerLocalize
import consulo.component.ProcessCanceledException
import consulo.execution.debug.frame.XCompositeNode
import consulo.internal.com.sun.jdi.{InvalidStackFrameException, ObjectCollectedException, VMDisconnectedException}
import consulo.localize.LocalizeValue
import consulo.logging.Logger
import consulo.util.lang.ControlFlowException
import org.jetbrains.plugins.scala.debugger.DebuggerBundle

import java.util.concurrent.{CancellationException, CompletionException}
import scala.util.control.NonFatal

package object util {

  private val LOG = Logger.getInstance(classOf[ScalaClassRenderer])

  /**
   * Builds the children of a debugger tree node with `build` and makes sure that the node finishes even if they cannot
   * be built: when `build` throws, the node shows an error message.
   * Nothing else reports such a failure, the node would stay at "Collecting data" for good.
   *
   * On success, `build` adds the children to the node itself.
   */
  // upstream: `build` returns a CompletableFuture, consulo-java renderers build the children synchronously
  def buildChildrenOrShowError(node: XCompositeNode)(build: => Unit): Unit =
    try build
    catch {
      case t: Throwable =>
        showChildrenError(node, t)
        if (t.isInstanceOf[ControlFlowException] || !NonFatal(t)) throw t
    }

  private def showChildrenError(node: XCompositeNode, failure: Throwable): Unit = {
    val cause = failure match {
      case e: CompletionException if e.getCause ne null => e.getCause
      case _ => failure
    }
    // the node is finished before logging, an error logged in tests throws
    if (!node.isObsolete) node.setErrorMessage(childrenErrorMessage(cause))
    cause match {
      case _: ControlFlowException => // must not be logged
      case _ if isExpectedChildrenFailure(cause) => LOG.debug(cause)
      case _ => LOG.error("Cannot build the children of a debugger tree node", cause)
    }
  }

  private def childrenErrorMessage(failure: Throwable): LocalizeValue = failure match {
    case _: CancellationException | _: ProcessCanceledException | _: InvalidStackFrameException | _: VMDisconnectedException =>
      JavaDebuggerLocalize.errorContextHasChanged()
    case e: EvaluateException if e.getMessage ne null => LocalizeValue.of(e.getMessage)
    case _ => LocalizeValue.of(DebuggerBundle.message("error.while.collecting.data", failure.toString))
  }

  // The debuggee and the debugger session may change under the renderer, these failures are not bugs
  private def isExpectedChildrenFailure(failure: Throwable): Boolean = failure match {
    case _: EvaluateException | _: CancellationException | _: InvalidStackFrameException | _: VMDisconnectedException |
         _: ObjectCollectedException => true
    case _ => false
  }
}
