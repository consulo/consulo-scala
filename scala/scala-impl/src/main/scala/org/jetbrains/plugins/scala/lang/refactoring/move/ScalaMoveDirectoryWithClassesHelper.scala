package org.jetbrains.plugins.scala.lang.refactoring.move

import consulo.annotation.component.ExtensionImpl

import consulo.project.Project
import com.intellij.java.language.psi.JavaPsiFacade
import consulo.language.psi.{PsiDirectory, PsiElement, PsiFile, PsiPackage}
import consulo.language.psi.scope.GlobalSearchScope
import consulo.language.psi.search.ReferencesSearch
import consulo.language.psi.util.PsiTreeUtil
import consulo.language.editor.refactoring.event.RefactoringElementListener
import com.intellij.java.impl.refactoring.move.moveClassesOrPackages.{CommonMoveUtil, MoveDirectoryWithClassesHelper}
import consulo.usage.MoveRenameUsageInfo
import consulo.usage.UsageInfo
import java.util.function.Function
import consulo.util.collection.ContainerUtil
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.scala.extensions.{ObjectExt, PsiElementExt}
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports.ScImportExpr
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.typedef.ScObjectImpl

import java.{util => ju}

@ExtensionImpl(order = "first")
class ScalaMoveDirectoryWithClassesHelper extends MoveDirectoryWithClassesHelper {

  override def findUsages(filesToMove: ju.Collection[PsiFile],
                          directoriesToMove: Array[PsiDirectory],
                          usages: ju.Collection[UsageInfo],
                          searchInComments: Boolean,
                          searchInNonJavaFiles: Boolean,
                          project: Project): Unit = {
    val packageNames = new ju.HashSet[String]
    filesToMove.forEach {
      case scalaFile: ScalaFile =>
        collectUsages(scalaFile, usages, searchInComments, searchInComments)
        packageNames.add(packageName(scalaFile))
      case _ =>
    }

    val psiFacade: JavaPsiFacade = JavaPsiFacade.getInstance(project)
    packageNames.forEach { packageName =>
      val aPackage: PsiPackage = psiFacade.findPackage(packageName)
      if (aPackage != null) {
        val remainsNothing: Boolean = aPackage.getDirectories.forall(isUnderRefactoring(_, directoriesToMove))

        if (remainsNothing) {
          ReferencesSearch.search(aPackage, GlobalSearchScope.projectScope(project)).findAll().forEach { reference =>
            reference.getElement.parentOfType(classOf[ScImportExpr])
              .foreach {
                case expr if !isUnderRefactoring(expr, directoriesToMove) && expr.hasWildcardSelector =>
                  usages.add(ImportExpressionToRemoveUsage(expr))
                case _ =>
              }
          }
        }
      }
    }
  }

  override def move(file: PsiFile,
                    moveDestination: PsiDirectory,
                    oldToNewElementsMapping: ju.Map[PsiElement, PsiElement],
                    movedFiles: ju.List[PsiFile],
                    listener: RefactoringElementListener): Boolean =
    moveFile(file, moveDestination, oldToNewElementsMapping, listener)

  override def postProcessUsages(usages: Array[UsageInfo], newDirMapper: Function[PsiDirectory, PsiDirectory]): Unit = {
    usages.foreach {
      case ImportExpressionToRemoveUsage(expr) =>
        // delete with parent statement if it is the only expression
        expr.deleteExpr()
      case _ =>
    }
  }

  override def afterMove(newElement: PsiElement): Unit = beforeMove(newElement.getContainingFile)

  override def beforeMove(file: PsiFile): Unit = file match {
    case scalaFile: ScalaFile =>
      scalaFile.typeDefinitions.foreach {
        collectAssociations(_, scalaFile, withCompanion = false)
      }
    case _ =>
  }

  // upstream: override of MoveDirectoryWithClassesHelper.retargetUsages (no such hook in Consulo)
  @NotNull
  def retargetUsages(@NotNull usages: ju.List[UsageInfo],
                              @NotNull oldToNewMap: ju.Map[PsiElement, PsiElement]): ju.List[UsageInfo] = {
    val usageInfosToProcess = ContainerUtil.filter[UsageInfo](usages, (usage: UsageInfo) => usage match {
      case usageInfo: MoveRenameUsageInfo =>
        val referencedElement = usageInfo.getUpToDateReferencedElement
        referencedElement != null && referencedElement.getContainingFile.is[ScalaFile]
      case _ => false
    })
    CommonMoveUtil.retargetUsages(usageInfosToProcess.toArray(UsageInfo.EMPTY_ARRAY), oldToNewMap)
    ContainerUtil.filter(usages, !usageInfosToProcess.contains(_))
  }

  private def isUnderRefactoring(element: PsiElement, directoriesToMove: Array[PsiDirectory]): Boolean =
    directoriesToMove.exists(PsiTreeUtil.isAncestor(_, element, /*strict*/ false))

  private def packageName(sf: ScalaFile) = {
    sf.typeDefinitions match {
      case Seq(obj: ScObject) if obj.isPackageObject =>
        if (obj.name == ScObjectImpl.LegacyPackageObjectNameInBackticks)
          ScObjectImpl.stripLegacyPackageObjectSuffixWithDot(obj.qualifiedName)
        else obj.qualifiedName
      case _ => sf.getPackageName
    }
  }
}

private case class ImportExpressionToRemoveUsage(expr: ScImportExpr) extends UsageInfo(expr)
