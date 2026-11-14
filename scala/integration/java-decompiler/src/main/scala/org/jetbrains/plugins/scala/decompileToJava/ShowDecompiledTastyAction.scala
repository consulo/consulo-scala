package org.jetbrains.plugins.scala.decompileToJava

import consulo.annotation.component.ActionImpl
import consulo.annotation.component.ActionParentRef
import consulo.annotation.component.ActionRef
import consulo.annotation.component.ActionRefAnchor

import consulo.language.psi.PsiNavigationSupport
import consulo.project.Project
import consulo.ui.ex.action.{ActionUpdateThread, AnAction, AnActionEvent}
import consulo.language.editor.CommonDataKeys
import consulo.virtualFileSystem.fileType.{FileType, FileTypeRegistry}
import consulo.virtualFileSystem.VirtualFile
import consulo.language.psi.util.PsiTreeUtil
import consulo.language.editor.util.PsiUtilBase
import com.intellij.java.language.psi.{PsiClass, PsiClassOwner}
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.decompileToJava.ShowDecompiledTastyAction.*
import org.jetbrains.plugins.scala.extensions.ObjectExt
import org.jetbrains.plugins.scala.tasty.TastyFileType

/**
 * The action shows the decompiled version of .tasty files in a format of readable Scala code
 *
 * This class was copied from [[org.jetbrains.java.decompiler.ShowDecompiledClassAction]]
 * with the exception that it handles .tasty files instead of .class files
 *
 * @see [[ShowDecompiledClassAsJavaAction]]
 * @see [[org.jetbrains.java.decompiler.ShowDecompiledClassAction]]
 */
@ActionImpl(id = "Scala.DecompileTasty", parents = Array(new ActionParentRef(value = new ActionRef(id = "QuickActions"), anchor = ActionRefAnchor.AFTER, relatedToAction = new ActionRef(id = "Scala.DecompileToJava"))))
class ShowDecompiledTastyAction extends AnAction(ScalaJavaDecompilerBundle.message("show.decompiled.tasty")) {

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  override def update(e: AnActionEvent): Unit = {
    showVisibleAndEnabled(e)
  }

  override def actionPerformed(e: AnActionEvent): Unit = {
    val project = e.getData(Project.KEY)
    if (project == null)
      return

    val tastyFile: VirtualFile = getOriginalTastyFile(e).orNull
    if (tastyFile == null)
      return

    PsiNavigationSupport.getInstance().createNavigatable(project, tastyFile, -1).navigate(true)
  }
}

object ShowDecompiledTastyAction {

  private[decompileToJava] def showVisibleAndEnabled(e: AnActionEvent): Unit = {
    val psiElement = getPsiElement(e)

    lazy val originalTastyFile = psiElement.flatMap(getOriginalTastyFile)

    val visible = psiElement.exists(_.getContainingFile.is[PsiClassOwner])
    val enabled = visible && originalTastyFile.isDefined

    e.getPresentation.setVisible(visible)
    e.getPresentation.setEnabled(enabled)
  }

  private[decompileToJava] def getPsiElement(e: AnActionEvent): Option[PsiElement] = {
    val project = e.getData(Project.KEY)
    if (project == null) return None

    val editor = e.getData(CommonDataKeys.EDITOR)
    if (editor != null) {
      val file = Option(PsiUtilBase.getPsiFileInEditor(editor, project))
      file.flatMap(file => Option(file.findElementAt(editor.getCaretModel.getOffset)))
    }
    else {
      Option(e.getData(CommonDataKeys.PSI_ELEMENT))
    }
  }

  private[decompileToJava] def getOriginalTastyFile(e: AnActionEvent): Option[VirtualFile] = {
    val psiElement = getPsiElement(e)
    psiElement.flatMap(getOriginalTastyFile)
  }

  private def getOriginalTastyFile(psiElement: PsiElement): Option[VirtualFile] =
    getOriginalFileOfType(psiElement, TastyFileType)

  private def getOriginalFileOfType(psiElement: PsiElement, fileType: FileType): Option[VirtualFile] = {
    val psiClass = PsiTreeUtil.getParentOfType(psiElement, classOf[PsiClass], false)
    val file = Option(psiClass).flatMap(cls => Option(cls.getOriginalElement.getContainingFile.getVirtualFile))
    file.filter(FileTypeRegistry.getInstance.isFileOfType(_, fileType))
  }
}