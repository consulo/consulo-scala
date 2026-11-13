package org.jetbrains.plugins.scala.gotoclass

import consulo.annotation.component.ExtensionImpl

import consulo.ide.navigation.GotoClassOrTypeContributor
import consulo.application.util.function.Processor
import consulo.content.scope.SearchScope
import consulo.language.psi.search.FindSymbolParameters
import consulo.language.psi.stub.IdFilter
import consulo.navigation.NavigationItem
import consulo.application.dumb.PossiblyDumbAware
import consulo.project.Project
import consulo.language.psi.scope.GlobalSearchScope
import consulo.project.content.scope.ProjectAwareSearchScope
import consulo.language.psi.stub.DumbModeAccessType
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition
import org.jetbrains.plugins.scala.lang.psi.stubs.index.ScalaIndexKeys
import org.jetbrains.plugins.scala.lang.refactoring.util.ScalaNamesUtil

@ExtensionImpl(order = "first")
class ScalaGoToClassContributor extends GotoClassOrTypeContributor with PossiblyDumbAware {

  import ScalaIndexKeys._

  override def getNames(project: Project, includeNonProjectItems: Boolean): Array[String] = {
    implicit val projectCopy: Project = project

    DumbModeAccessType.RAW_INDEX_DATA_ACCEPTABLE.ignoreDumbMode { () =>
      (NOT_VISIBLE_IN_JAVA_SHORT_NAME_KEY.allKeys ++ PACKAGE_OBJECT_SHORT_NAME_KEY.allKeys).toArray
    }
  }

  override def getItemsByName(name: String, pattern: String, project: Project, includeNonProjectItems: Boolean): Array[NavigationItem] = {
    val scope = if (includeNonProjectItems) GlobalSearchScope.allScope(project) else GlobalSearchScope.projectScope(project)
    val cleanName = ScalaNamesUtil.cleanFqn(name)

    implicit val projectCopy: Project = project

    DumbModeAccessType.RELIABLE_DATA_ONLY.ignoreDumbMode { () =>
      val classes = NOT_VISIBLE_IN_JAVA_SHORT_NAME_KEY.elements(cleanName, scope)
      val packageObjects = PACKAGE_OBJECT_SHORT_NAME_KEY.elements(cleanName, scope)
      (classes ++ packageObjects).toArray[NavigationItem]
    }
  }

  override def getQualifiedName(item: NavigationItem): String = item match {
    case t: ScTypeDefinition => t.qualifiedName
    case _ => null
  }

  override def getQualifiedNameSeparator: String = "."


  override def processNames(processor: Processor[String], scope: SearchScope, filter: IdFilter): Unit = {
    val project = scope.asInstanceOf[ProjectAwareSearchScope].getProject
    val includeNonProjectItems = scope match {
      case globalScope: GlobalSearchScope => globalScope.isSearchInLibraries
      case _ => true
    }
    getNames(project, includeNonProjectItems).iterator.takeWhile(processor.process).foreach(_ => ())
  }

  override def processElementsWithName(name: String, processor: Processor[NavigationItem], parameters: FindSymbolParameters): Unit =
    getItemsByName(name, parameters.getCompletePattern, parameters.getProject, parameters.isSearchInLibraries)
      .iterator.takeWhile(processor.process).foreach(_ => ())

  override def isDumbAware: Boolean = true
}
