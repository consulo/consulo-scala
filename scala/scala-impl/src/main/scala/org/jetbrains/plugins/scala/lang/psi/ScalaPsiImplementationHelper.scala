package org.jetbrains.plugins.scala.lang.psi

import consulo.content.bundle.Sdk
import consulo.content.library.Library
import consulo.module.content.layer.orderEntry.{LibraryOrderEntry, ModuleExtensionWithSdkOrderEntry}
import consulo.module.Module
import consulo.project.Project
import consulo.module.content.{ProjectFileIndex, ProjectRootManager}
import consulo.virtualFileSystem.VirtualFile
import consulo.language.psi.scope.GlobalSearchScope
import com.intellij.java.language.psi.PsiClass
import consulo.language.psi.PsiFile
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScClass, ScObject, ScTrait}
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiManager

import scala.jdk.CollectionConverters.CollectionHasAsScala

object ScalaPsiImplementationHelper {
  def getOriginalClass(psiClass: PsiClass): PsiClass = {
    val psiFile: PsiFile = psiClass.getContainingFile
    val vFile: VirtualFile = psiFile.getVirtualFile
    val project: Project = psiClass.getProject
    val idx: ProjectFileIndex = ProjectRootManager.getInstance(project).getFileIndex
    if (vFile == null || !idx.isInLibrarySource(vFile)) return psiClass
    val originalEntities = findAssociatedEntities(vFile, idx).toList
    val fqn: String = psiClass.qualifiedName
    if (fqn == null) return psiClass
    val classes: Array[PsiClass] = ScalaPsiManager.instance(project).getCachedClasses(new GlobalSearchScope(project) {
      override def compare(file1: VirtualFile, file2: VirtualFile): Int = 0
      override def contains(file: VirtualFile): Boolean = {
        val myEntities = findAssociatedEntities(file, idx)
        for (element <- myEntities) {
          if (originalEntities.contains(element)) return true
        }
        false
      }
      override def isSearchInModuleContent(aModule: Module): Boolean = false
      override def isSearchInLibraries: Boolean = true
    }, fqn)
    if (classes.length == 0) psiClass
    else if (classes.length == 1) classes(0)
    else {
      psiClass match {
        case _: ScTrait | _: ScClass =>
          classes.find(td => td.isInstanceOf[ScTrait] || td.isInstanceOf[ScClass]).getOrElse(classes(0))
        case _: ScObject =>
          classes.find(td => td.isInstanceOf[ScObject]).getOrElse(classes(0))
        case _ => classes(0)
      }
    }
  }

  /**
   * Searches for libraries or SDKs associated with a given file (upstream: workspace model entities).
   *
   * First, it searches for [[Library]]s associated with the file. If no libraries are found,
   * it searches for [[Sdk]]s associated with the file.
   */
  def findAssociatedEntities(file: VirtualFile, idx: ProjectFileIndex): Iterable[AnyRef] = {
    val orderEntries = idx.getOrderEntriesForFile(file).asScala
    val libraries = orderEntries.collect { case entry: LibraryOrderEntry if entry.getLibrary != null => entry.getLibrary }

    if (libraries.nonEmpty) {
      libraries
    } else {
      orderEntries.collect { case entry: ModuleExtensionWithSdkOrderEntry if entry.getSdk != null => entry.getSdk }
    }
  }
}
