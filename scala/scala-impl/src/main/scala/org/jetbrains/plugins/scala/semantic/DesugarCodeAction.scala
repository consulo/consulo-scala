package org.jetbrains.plugins.scala.semantic

import consulo.project.Project
import consulo.diff.request.SimpleDiffRequest
import consulo.diff.{DiffContentFactory, DiffManager}
import consulo.ui.ex.action.AnActionEvent
import consulo.language.editor.CommonDataKeys
import consulo.codeEditor.Editor
import consulo.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorOpenRequest
import consulo.module.Module
import com.intellij.openapi.roots.CompilerModuleExtension
import consulo.module.content.layer.orderEntry.LibraryOrderEntry
import consulo.module.content.{ModuleRootManager, ProjectFileIndex}
import consulo.virtualFileSystem.util.{VirtualFileUtil => VfsUtil}
import com.intellij.java.language.psi.PsiClass
import consulo.language.editor.refactoring.util.CommonRefactoringUtil
import consulo.language.file.light.LightVirtualFile
import org.jetbrains.plugins.scala.extensions.{PsiElementExt, inReadAction, withProgressSynchronously}
import org.jetbrains.plugins.scala.icons.Icons
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition
import org.jetbrains.plugins.scala.project.ProjectPsiFileExt
import org.jetbrains.plugins.scala.settings.ScalaApplicationSettings.{getInstance => ScalaApplicationSettings}
import org.jetbrains.plugins.scala.api.{LanguageFileTypeBase, Scala3Language}

import java.nio.file.Files
import javax.swing.Icon
import scala.jdk.CollectionConverters.CollectionHasAsScala

object DesugarCodeAction {
  private def getTemplateText: String = "Desugar Scala Code"

  def actionPerformed(e: AnActionEvent): Unit = {
    val project = e.getData(Project.KEY)
    val editor = e.getDataContext.getData(CommonDataKeys.EDITOR)
    val document = editor.getDocument
    val virtualFile = editor.getVirtualFile
    val psiFile = e.getDataContext.getData(CommonDataKeys.PSI_FILE).asInstanceOf[ScalaFile]
    if (psiFile.isCompiled) {
      CommonRefactoringUtil.showErrorHint(project, editor, "File must have source", getTemplateText, null)
      return
    }
    val cls = classAtCaret(editor, psiFile).orElse(psiFile.typeDefinitions.headOption).getOrElse {
      CommonRefactoringUtil.showErrorHint(project, editor, "No class to desugar", getTemplateText, null)
      return
    }

    val isScala3 = cls.isInScala3File

    val fileIndex = ProjectFileIndex.getInstance(project)

    val (tastyFileName, tastyFileContents, upToDate, classpath) = if (!isScala3) ("", Array.emptyByteArray, true, Seq.empty[String]) else if (fileIndex.isInSourceContent(virtualFile)) {
      val module = psiFile.module.getOrElse(throw new RuntimeException(s"No module for $psiFile"))
      val outputDir =
        if (fileIndex.isInTestSourceContent(virtualFile)) CompilerModuleExtension.getInstance(module).getCompilerOutputPathForTests.toNioPath
        else CompilerModuleExtension.getInstance(module).getCompilerOutputPath.toNioPath
      val tastyFile = {
        val elements = (cls: PsiClass).getQualifiedName.split('.').toSeq.dropRight(1) :+ (cls: PsiClass).getName.stripSuffix("$") + ".tasty"
        elements.foldLeft(outputDir)((acc, x) => acc.resolve(x))
      }
      if (!Files.exists(tastyFile)) {
        CommonRefactoringUtil.showErrorHint(project, editor, s"${outputDir.relativize(tastyFile)} not found; please compile the class", getTemplateText, null)
        return
      }
      val upToDate = document.getModificationStamp <= virtualFile.getModificationStamp && virtualFile.getTimeStamp <= Files.getLastModifiedTime(tastyFile).toMillis
      (tastyFile.getFileName.toString, Files.readAllBytes(tastyFile), upToDate, classpathOf(module))
    } else {
      val compiledPsiFile = cls.getOriginalElement.getContainingFile
      val compiledVirtualFile = compiledPsiFile.getOriginalFile.getVirtualFile
      val module = fileIndex.getOrderEntriesForFile(compiledVirtualFile).asScala.collectFirst { case entry: LibraryOrderEntry => entry.getOwnerModule }.getOrElse {
        CommonRefactoringUtil.showErrorHint(project, editor, "Project must have a module that depends on this library", getTemplateText, null)
        return
      }
      (compiledVirtualFile.getName, compiledVirtualFile.contentsToByteArray(), true, classpathOf(module))
    }

    val (compilerText, pluginText) = withProgressSynchronously(s"Desugaring ${cls.name}...") {
      val compilerText = if (!isScala3) "" else {
        val decompiler = Decompiler(classpath, Decompiler.classLoader(getClass.getClassLoader))
        decompiler.decompile(tastyFileName, tastyFileContents)
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
      val file = new LightVirtualFile(s"${cls.name} (desugared)", psiFile.getFileType, pluginText)
      FileEditorManager.getInstance(project).requestOpenFile(file, FileEditorOpenRequest.withFocus(true))
    } else {
      val left = DiffContentFactory.getInstance.create(project, compilerText, Scala3FileType)
      val right = DiffContentFactory.getInstance.create(project, pluginText, Scala3FileType)
      DiffManager.getInstance.showDiff(project, new SimpleDiffRequest("Desugaring of " + cls.qualifiedName, left, right, "Compiler" + (if (upToDate) "" else " (outdated, please recompile):"), "Plugin:"))
    }
  }

  private def classpathOf(module: Module): Seq[String] =
    ModuleRootManager.getInstance(module)
      .orderEntries.withoutSdk.classes.getRoots.toSeq
      .map(VfsUtil.getLocalFile(_).getPath)

  private def classAtCaret(editor: Editor, file: ScalaFile): Option[ScTypeDefinition] = {
    val offset = editor.getCaretModel.getOffset
    val elementAtCaret = Option(file.findElementAt(offset))
    val classAtCaret = elementAtCaret.flatMap(_.contexts.collectFirst { case td: ScTypeDefinition if td.isTopLevel => td })
    classAtCaret
  }

  private object Scala3FileType extends LanguageFileTypeBase(Scala3Language.INSTANCE) {
    override def getIcon: Icon = Icons.SCALA_FILE
  }
}
