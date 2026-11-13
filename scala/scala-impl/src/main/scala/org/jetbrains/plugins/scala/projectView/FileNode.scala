package org.jetbrains.plugins.scala
package projectView


import org.jetbrains.plugins.scala.api.ScalaFileType
import consulo.project.ui.view.tree.PsiFileNode
import consulo.ui.ex.tree.PresentationData
import consulo.project.ui.view.tree.ViewSettings
import consulo.virtualFileSystem.fileType.FileType
import consulo.project.Project
import org.jetbrains.plugins.scala.icons.Icons
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.tasty.TastyFileType

import java.{util => ju}
import javax.swing.Icon

private[projectView] sealed abstract class FileNode(protected val file: ScalaFile)
                                                   (implicit project: Project, settings: ViewSettings)
  extends PsiFileNode(project, file, settings) with IconableNode {

  import scala.jdk.CollectionConverters._

  override def getChildrenImpl: ju.Collection[Node] =
    if (settings.isShowMembers)
      file.members.flatMap(buildMemberNodes(_)).asJava
    else
      emptyNodesList

  override protected def updateImpl(data: PresentationData): Unit = {
    super.updateImpl(data)
    setIcon(data)
  }
}

private[projectView] object FileNode {

  final class ScalaFileNode(override protected val file: ScalaFile)
                           (implicit project: Project, settings: ViewSettings)
    extends FileNode(file) {

    override def getIcon(flags: Int): Icon = ScalaFileType.INSTANCE.getIcon

    override protected def updateImpl(data: PresentationData): Unit = {
      super.updateImpl(data)

      val presentableText = file.getName
        .stripSuffix(ScalaFileType.INSTANCE.getExtensionWithDot)
        .stripSuffix("$package." + TastyFileType.getDefaultExtension) // Compiled top-level definitions

      data.setPresentableText(presentableText)
    }
  }

  final class DialectFileNode(override protected val file: ScalaFile,
                              fileType: FileType)
                             (implicit project: Project, settings: ViewSettings)
    extends FileNode(file) {

    override def getIcon(flags: Int): Icon = fileType.getIcon
  }

  final class MillFileNode(override protected val file: ScalaFile)
                          (implicit project: Project, settings: ViewSettings)
    extends FileNode(file) {

    override def getIcon(flags: Int): Icon = Icons.MILL_FILE
  }
}