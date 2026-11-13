package org.jetbrains.plugins.scala.lang.refactoring.move

import consulo.annotation.component.ExtensionImpl

import consulo.logging.Logger
import consulo.module.content.ProjectRootManager
import consulo.language.psi.{PsiDirectory, PsiElement, PsiFile}
import consulo.language.editor.refactoring.move.MoveFileHandler
import consulo.usage.MoveRenameUsageInfo
import consulo.usage.UsageInfo
import consulo.language.util.IncorrectOperationException
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.scala.editor.importOptimizer.ScalaImportOptimizer
import org.jetbrains.plugins.scala.extensions.ObjectExt
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.statistics.ScalaRefactoringUsagesCollector

import java.util
import scala.jdk.CollectionConverters.IterableHasAsScala

@ExtensionImpl(order = "first")
class MoveScalaFileHandler extends MoveFileHandler {
  override def canProcessElement(element: PsiFile): Boolean = {
    if (!element.is[ScalaFile])
      return false
    val file = element.getVirtualFile
    if (file == null)
      return false

    val projectFileIndex = ProjectRootManager.getInstance(element.getProject).getFileIndex
    projectFileIndex.isInSource(file) || projectFileIndex.isInLibraryClasses(file)
  }

  override def prepareMovedFile(file: PsiFile, moveDestination: PsiDirectory, oldToNewMap: util.Map[PsiElement, PsiElement]): Unit = file match {
    case file: ScalaFile =>
      ScalaRefactoringUsagesCollector.logMoveFile(file.getProject)
      file.typeDefinitions.foreach(collectAssociations(_, file, withCompanion = false))

      moveFile(file, moveDestination, oldToNewMap)
    case _ =>
  }

  override def findUsages(psiFile: PsiFile, newParent: PsiDirectory, searchInComments: Boolean, searchInNonJavaFiles: Boolean): util.List[UsageInfo] =
    psiFile match {
      case scalaFile: ScalaFile =>
        val result = new util.ArrayList[UsageInfo]
        collectUsages(scalaFile, result, searchInComments, searchInNonJavaFiles)

        if (result.isEmpty) null else result
      case _ => null
    }

  override def retargetUsages(@NotNull usageInfos: util.List[UsageInfo],
                              @NotNull oldToNewMap: util.Map[PsiElement, PsiElement]): Unit = {
    for (usage <- usageInfos.asScala) {
      usage match {
        case moveRenameUsage: MoveRenameUsageInfo =>
          val oldElement = moveRenameUsage.getReferencedElement
          val newElement = oldToNewMap.get(oldElement)
          val reference = moveRenameUsage.getReference
          if (reference != null) try {
            MoveScalaFileHandler.LOG.assertTrue(newElement != null, if (oldElement != null) oldElement else reference)
            reference.bindToElement(newElement)
          } catch {
            case ex: IncorrectOperationException =>
              MoveScalaFileHandler.LOG.error(ex)
          }
        case _ =>
      }
    }
  }

  @throws[IncorrectOperationException]
  override def updateMovedFile(file: PsiFile): Unit = file match {
    case file: ScalaFile =>
      file.typeDefinitions.foreach(restoreAssociations)

      new ScalaImportOptimizer().processFile(file).run()
    case _ =>
  }
}

object MoveScalaFileHandler {
  private val LOG = Logger.getInstance("#org.jetbrains.plugins.scala.lang.refactoring.move.MoveScalaFileHandler")
}
