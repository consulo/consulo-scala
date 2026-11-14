package org.jetbrains.plugins.scala.debugger.breakpoints

import consulo.annotation.access.RequiredReadAction
import consulo.annotation.component.ExtensionImpl
import consulo.execution.debug.breakpoint.{XBreakpointType, XLineBreakpointType, XLineBreakpointTypeResolver}
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import consulo.virtualFileSystem.fileType.FileType
import org.jetbrains.plugins.scala.api.ScalaFileType

@ExtensionImpl
class ScalaLineBreakpointTypeResolver extends XLineBreakpointTypeResolver {

  @RequiredReadAction
  override def resolveBreakpointType(project: Project, virtualFile: VirtualFile, line: Int): XLineBreakpointType[?] = {
    val breakpointType = XBreakpointType.EXTENSION_POINT_NAME.findExtensionOrFail(classOf[ScalaLineBreakpointType])
    if (breakpointType.canPutAt(virtualFile, line, project)) breakpointType else null
  }

  override def getFileType: FileType = ScalaFileType.INSTANCE
}
