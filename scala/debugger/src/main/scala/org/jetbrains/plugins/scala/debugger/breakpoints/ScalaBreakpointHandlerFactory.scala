package org.jetbrains.plugins.scala.debugger.breakpoints

import consulo.annotation.component.ExtensionImpl

import com.intellij.debugger.engine.{DebugProcessImpl, JavaBreakpointHandler, JavaBreakpointHandlerFactory}

@ExtensionImpl
class ScalaBreakpointHandlerFactory extends JavaBreakpointHandlerFactory {
  override def createHandler(process: DebugProcessImpl): JavaBreakpointHandler = new ScalaBreakpointHandler(process)
}

class ScalaBreakpointHandler(process: DebugProcessImpl) extends JavaBreakpointHandler(classOf[ScalaLineBreakpointType], process)