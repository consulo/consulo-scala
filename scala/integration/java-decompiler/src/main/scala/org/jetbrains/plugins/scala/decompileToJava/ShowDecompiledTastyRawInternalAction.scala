package org.jetbrains.plugins.scala.decompileToJava

import consulo.annotation.component.ActionImpl
import consulo.annotation.component.ActionParentRef
import consulo.annotation.component.ActionRef
import consulo.annotation.component.ActionRefAnchor

import consulo.ui.ex.action.{ActionUpdateThread, AnAction, AnActionEvent}
import consulo.navigation.OpenFileDescriptor
import com.intellij.openapi.fileTypes.PlainTextFileType
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import consulo.language.file.light.LightVirtualFile
import org.jetbrains.plugins.scala.extensions.{executeOnPooledThread, invokeLater}
import org.jetbrains.plugins.scala.tasty.reader.{NameTable, NodePrinter, TreeReader}

//NOTE: disabled inspection as it's an internal action
//noinspection ScalaExtractStringToBundle
@ActionImpl(id = "Scala.DecompileTastyRaw", parents = Array(new ActionParentRef(value = new ActionRef(id = "idea.java.decompiler.action.group"), anchor = ActionRefAnchor.AFTER, relatedToAction = new ActionRef(id = "Scala.DecompileTasty"))))
class ShowDecompiledTastyRawInternalAction extends AnAction("Show Tasty Raw Representation (Internal)") {

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  override def update(e: AnActionEvent): Unit = {
    ShowDecompiledTastyAction.showVisibleAndEnabled(e)
  }

  override def actionPerformed(e: AnActionEvent): Unit = {
    val project = e.getProject
    if (project == null)
      return

    val tastyFile = ShowDecompiledTastyAction.getOriginalTastyFile(e).orNull
    if (tastyFile == null)
      return

    // Run in the background primarily to be able to use Dev IDEA while debugging the tasty reading logic (the action is internal)
    executeOnPooledThread {
      val tastyFileContent: Array[Byte] = tastyFile.contentsToByteArray

      val (node, nameTable) = TreeReader.treeAndNameTableFrom(tastyFileContent)

      val nameTableText = printNameTable(nameTable)
      val rootNodeText = new NodePrinter(printAddress = true).print(node)

      val tastyFilePresentableText =
        s"""Trees
           |(Note, shared nodes can be inlined into other nodes, so the tree structure may not be exactly the same as with -Yprint-tasty)
           |$rootNodeText
           |
           |Names
           |$nameTableText
           |""".stripMargin

      invokeLater {
        createDummyFileWithTextAndOpenEditor(project, fileText = tastyFilePresentableText, tastyFile)
      }
    }
  }

  private def printNameTable(nameTable: NameTable) = {
    nameTable.contents.zipWithIndex.map { (termName, index) =>
      f"$index%6s: ${termName.toString()}"
    }.mkString("\n")
  }

  private def createDummyFileWithTextAndOpenEditor(project: Project, fileText: String, tastyFile: VirtualFile): Unit = {
    val fileName = tastyFile.getNameWithoutExtension + ".tasty.raw"
    val lightFile = new LightVirtualFile(fileName, PlainTextFileType.INSTANCE, fileText)
    new OpenFileDescriptor(project, lightFile).navigate(true)
  }
}