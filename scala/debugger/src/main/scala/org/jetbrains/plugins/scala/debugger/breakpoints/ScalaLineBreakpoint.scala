package org.jetbrains.plugins.scala.debugger.breakpoints

import com.intellij.java.debugger.impl.breakpoints.properties.JavaLineBreakpointProperties
import com.intellij.java.debugger.impl.engine.DebugProcessImpl
import com.intellij.java.debugger.impl.ui.breakpoints.LineBreakpoint
import consulo.execution.debug.breakpoint.XBreakpoint
import consulo.internal.com.sun.jdi.{ClassNotPreparedException, ObjectCollectedException, ReferenceType}
import consulo.logging.Logger
import consulo.project.Project
import org.jetbrains.plugins.scala.debugger.ScalaConditionalReturn

import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal

/**
 * A line breakpoint that suspends on the return instructions of a conditional (early) return (SCL-21626).
 * upstream: done by the IntelliJ LineBreakpoint itself, consulo-java's LineBreakpoint has no conditional returns.
 */
private final class ScalaLineBreakpoint(project: Project, xBreakpoint: XBreakpoint[?])
  extends LineBreakpoint[JavaLineBreakpointProperties](project, xBreakpoint) {

  override protected def createRequestForPreparedClass(debugProcess: DebugProcessImpl, classType: ReferenceType): Unit = {
    val position = getSourcePosition
    val vm = classType.virtualMachine
    if (!ScalaLineBreakpointType.isConditionalReturn(getXBreakpoint) || position == null || !vm.canGetBytecodes || !vm.canGetConstantPool) {
      super.createRequestForPreparedClass(debugProcess, classType)
      return
    }

    // upstream: also checks that the class is in the scope of the debug process (LineBreakpoint.isInScopeOf is private)
    try {
      val returnLocations = debugProcess.getPositionManager.locationsOfLine(classType, position).asScala.toSeq
        .filter(acceptLocation(debugProcess, classType, _))
        .map(_.method)
        .distinct
        .flatMap(ScalaConditionalReturn.inlineConditionalReturnLocations(_, position.getLine + 1))
      val requestsManager = debugProcess.getRequestsManager
      returnLocations.foreach { location =>
        requestsManager.enableRequest(requestsManager.createBreakpointRequest(this, location))
      }
    } catch {
      case _: ClassNotPreparedException | _: ObjectCollectedException =>
        // there's a chance to add a breakpoint when the class is prepared
      case NonFatal(e) =>
        ScalaLineBreakpoint.Log.info(e)
    }
    updateUI()
  }
}

private object ScalaLineBreakpoint {
  private val Log = Logger.getInstance(classOf[ScalaLineBreakpoint])
}
