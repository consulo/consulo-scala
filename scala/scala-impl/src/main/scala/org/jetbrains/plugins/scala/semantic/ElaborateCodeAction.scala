package org.jetbrains.plugins.scala.semantic

import consulo.ui.image.Image
import consulo.language.content.{ProductionContentFolderTypeProvider, TestContentFolderTypeProvider}
import consulo.compiler.ModuleCompilerPathsManager
import consulo.project.Project
import consulo.diff.request.SimpleDiffRequest
import consulo.diff.{DiffContentFactory, DiffManager}
import consulo.ui.ex.action.{AnAction, AnActionEvent, AnActionWithSyncUpdate}
import consulo.annotation.component.{ActionImpl, ActionParentRef, ActionRef}
import consulo.language.editor.CommonDataKeys
import consulo.codeEditor.Editor
import consulo.fileEditor.FileEditorManager
import consulo.module.Module
import consulo.module.content.layer.orderEntry.LibraryOrderEntry
import consulo.module.content.{ModuleRootManager, ProjectFileIndex}
import consulo.virtualFileSystem.util.VirtualFilePathUtil
import com.intellij.java.language.psi.PsiClass
import consulo.language.editor.refactoring.util.CommonRefactoringUtil
import consulo.language.file.light.LightVirtualFile
import org.jetbrains.plugins.scala.ScalaBundle.message
import org.jetbrains.plugins.scala.actions.ScalaActionUtil
import org.jetbrains.plugins.scala.extensions.{PsiElementExt, inReadAction, withProgressSynchronously}
import org.jetbrains.plugins.scala.icons.Icons
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition
import org.jetbrains.plugins.scala.project.ProjectPsiFileExt
import org.jetbrains.plugins.scala.settings.ScalaApplicationSettings.{getInstance => ScalaApplicationSettings}
import org.jetbrains.plugins.scala.statistics.ScalaActionUsagesCollector
import org.jetbrains.plugins.scala.api.{LanguageFileTypeBase, Scala3Language}

import java.nio.file.Files
import scala.jdk.CollectionConverters.CollectionHasAsScala

@ActionImpl(id = "Scala.ElaborateCode", parents = Array(new ActionParentRef(value = new ActionRef(id = "CodeMenu")), new ActionParentRef(value = new ActionRef(id = "EditorPopupMenu"))))
class ElaborateCodeAction extends AnAction(message("elaborate.code.action.text"), message("elaborate.code.action.description"), /* icon = */ null)
  with AnActionWithSyncUpdate {

  override def update(e: AnActionEvent): Unit =
    ScalaActionUtil.enableAndShowIfInScalaFile(e)

  override def actionPerformed(e: AnActionEvent): Unit = {
    val project = e.getData(Project.KEY)
    val editor = e.getDataContext.getData(CommonDataKeys.EDITOR)
    val psiFile = e.getDataContext.getData(CommonDataKeys.PSI_FILE).asInstanceOf[ScalaFile]
    perform(project, editor, psiFile)
  }

  def perform(project: Project, editor: Editor, psiFile: ScalaFile): Unit = {
    ScalaActionUsagesCollector.logDesugarCode(project)

    val document = editor.getDocument
    val virtualFile = editor.getVirtualFile
    if (psiFile.isCompiled) {
      CommonRefactoringUtil.showErrorHint(project, editor, message("elaborate.code.no.source"), getTemplateText, null)
      return
    }
    val cls = classAtCaret(editor, psiFile).orElse(psiFile.typeDefinitions.headOption).getOrElse {
      CommonRefactoringUtil.showErrorHint(project, editor, message("elaborate.code.no.class"), getTemplateText, null)
      return
    }

    val isScala3 = cls.isInScala3File

    val fileIndex = ProjectFileIndex.getInstance(project)

    val (tastyFileName, tastyFileContents, upToDate, classpath) = if (!isScala3) ("", Array.emptyByteArray, true, Seq.empty[String]) else if (fileIndex.isInSourceContent(virtualFile)) {
      val module = psiFile.module.getOrElse(throw new RuntimeException(s"No module for $psiFile"))
      val outputDir =
        if (fileIndex.isInTestSourceContent(virtualFile)) ModuleCompilerPathsManager.getInstance(module).getCompilerOutputPath(TestContentFolderTypeProvider.getInstance())
        else ModuleCompilerPathsManager.getInstance(module).getCompilerOutputPath(ProductionContentFolderTypeProvider.getInstance())
      val tastyFile = {
        val elements = (cls: PsiClass).getQualifiedName.split('.').toSeq.dropRight(1) :+ (cls: PsiClass).getName.stripSuffix("$") + ".tasty"
        elements.foldLeft(outputDir)((acc, x) => acc.resolve(x))
      }
      if (!Files.exists(tastyFile)) {
        CommonRefactoringUtil.showErrorHint(project, editor, message("elaborate.code.no.file", outputDir.relativize(tastyFile)), getTemplateText, null)
        return
      }
      val upToDate = document.getModificationStamp <= virtualFile.getModificationStamp && virtualFile.getTimeStamp <= Files.getLastModifiedTime(tastyFile).toMillis
      (tastyFile.getFileName.toString, Files.readAllBytes(tastyFile), upToDate, classpathOf(module))
    } else {
      val compiledPsiFile = cls.getOriginalElement.getContainingFile
      val compiledVirtualFile = compiledPsiFile.getOriginalFile.getVirtualFile
      val module = fileIndex.getOrderEntriesForFile(compiledVirtualFile).asScala.collectFirst { case entry: LibraryOrderEntry => entry.getOwnerModule }.getOrElse {
        CommonRefactoringUtil.showErrorHint(project, editor, message("elaborate.code.no.module"), getTemplateText, null)
        return
      }
      (compiledVirtualFile.getName, compiledVirtualFile.contentsToByteArray(), true, classpathOf(module))
    }

    val (compilerText, pluginText) = withProgressSynchronously(message("elaborate.code.elaborating", cls.name)) {
      val compilerText = if (!isScala3) "" else {
        val decompiler = Decompiler(classpath, Decompiler.classLoader(getClass.getClassLoader))
        decompiler.decompile(tastyFileName, tastyFileContents.toIndexedSeq)
      }
      val pluginText = inReadAction {
        try {
          ScalaApplicationSettings.PRECISE_TEXT = true
          ScalaApplicationSettings.PRECISE_TEXT_FOR_TYPE_PARAMETERS = isScala3
          ClassPrinter.textOf(cls, isScala3 = isScala3)
        } finally {
          ScalaApplicationSettings.PRECISE_TEXT = false
          ScalaApplicationSettings.PRECISE_TEXT_FOR_TYPE_PARAMETERS = false
        }
      }
      (compilerText, pluginText)
    }

    if (!isScala3) {
      val file = new LightVirtualFile(message("elaborate.code.elaborated", cls.name), psiFile.getFileType, pluginText)
      FileEditorManager.getInstance(project).openFile(file, true)
    } else {
      val leftTitle = s"${message("elaborate.code.compiler")}${if (upToDate) "" else s" ⚠️${message("elaborate.code.outdated")}"}"
      val rightTitle = s"${message("elaborate.code.plugin")}"
      val left = DiffContentFactory.getInstance.create(project, compilerText, Scala3FileType)
      val right = DiffContentFactory.getInstance.create(project, pluginText, Scala3FileType)
      DiffManager.getInstance.showDiff(project, new SimpleDiffRequest(message("elaborate.code.elaboration", cls.qualifiedName), left, right, leftTitle, rightTitle))
    }
  }

  private def classpathOf(module: Module): Seq[String] =
    ModuleRootManager.getInstance(module)
      .orderEntries.withoutSdk.classes.getRoots.toSeq
      .map(VirtualFilePathUtil.getLocalFile(_).getPath)

  private def classAtCaret(editor: Editor, file: ScalaFile): Option[ScTypeDefinition] = {
    val offset = editor.getCaretModel.getOffset
    val elementAtCaret = Option(file.findElementAt(offset))
    val classAtCaret = elementAtCaret.flatMap(_.contexts.collectFirst { case td: ScTypeDefinition if td.isTopLevel => td })
    classAtCaret
  }

  private object Scala3FileType extends LanguageFileTypeBase(Scala3Language.INSTANCE) {
    override def getIcon: Image = Icons.SCALA_FILE
  }
}
