package org.jetbrains.sbt
package language


import org.jetbrains.sbt.api.language.SbtFile
import org.jetbrains.sbt.api.language.SbtFileType
import consulo.module.Module
import consulo.language.util.ModuleUtilCore
import consulo.module.content.ProjectRootManager
import consulo.language.file.FileViewProvider
import consulo.language.psi.PsiElement
import consulo.language.psi.resolve.ResolveState
import consulo.language.psi.scope.GlobalSearchScope
import com.intellij.java.indexing.search.searches.ClassInheritorsSearch
import consulo.language.psi.resolve.PsiScopeProcessor
import org.jetbrains.annotations.NonNls
import org.jetbrains.plugins.scala.caches.{ModTracker, cached, cachedInUserData}
import org.jetbrains.plugins.scala.extensions.PsiClassExt
import org.jetbrains.plugins.scala.lang.psi.ScDeclarationSequenceHolder
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition
import org.jetbrains.plugins.scala.lang.psi.impl.*
import org.jetbrains.plugins.scala.project.ProjectPsiElementExt
import org.jetbrains.sbt.project.module.SbtModule.Imports

import scala.jdk.CollectionConverters.*

final class SbtFileImpl private[language](provider: FileViewProvider)
  extends ScalaFileImpl(provider, SbtFileType)
    with SbtFile
    with ScDeclarationSequenceHolder {

  import SbtFileImpl._

  override def typeDefinitions: Seq[ScTypeDefinition] = Seq.empty

  override val allowsForwardReferences: Boolean = true

  override def processDeclarations(processor: PsiScopeProcessor,
                                   state: ResolveState,
                                   lastParent: PsiElement,
                                   place: PsiElement): Boolean =
    super[ScalaFileImpl].processDeclarations(processor, state, lastParent, place) &&
      super[ScDeclarationSequenceHolder].processDeclarations(processor, state, lastParent, place) &&
      syntheticFile().forall { file =>
        file.processDeclarations(processor, state, file.getLastChild, place)
      }

  private val syntheticFile = cached("syntheticFile", ModTracker.physicalPsiChange(getProject), () => {
    implicit val manager: ScalaPsiManager = ScalaPsiManager.instance(getProject)
    @NonNls val imports = importsFor(targetModule).map {
      // TODO this is a workaround, we need to find out why references stopped resolving via the chained imports
      case "Keys._" => "sbt.Keys._"
      case "Build._" => "sbt.Build._"
      // TODO: this is a workaround. `processDeclarations` does not resolve "Play.autoImport -> PlayImport"
      //    However, when object with implicit imports is located in the same file where plugin object resides
      //    everything is resolved, but PlayImport and Play are in different files.
      case "_root_.play.Play.autoImport._" => "_root_.play.PlayImport._"
      case importText => importText
    }

    if (imports.isEmpty) None
    else Some(ScalaPsiElementFactory.createScalaFileFromText(imports.mkString("import ", ", ", ";"), this.features))
  })

  /**
   * @note dual logic is located in [[org.jetbrains.sbt.language.psi.search.SbtBuildModuleUseScopeEnlarger]]
   * @todo consider rewriting this using [[com.intellij.psi.ResolveScopeEnlarger]]
   */
  override def getFileResolveScope: GlobalSearchScope = {
    val target = targetModule
    target match {
      case SbtModuleWithScope(_, moduleWithDependenciesAndLibrariesScope) =>
        moduleWithDependenciesAndLibrariesScope
      case _ =>
        super.getFileResolveScope
    }
  }

  private def targetModule: TargetModule = cachedInUserData("targetModule", this, ProjectRootManager.getInstance(getProject)) {
    val moduleForFile = ModuleUtilCore.findModuleForPsiElement(this)
    moduleForFile match {
      case null =>
        ModuleLess
      case module =>
        val buildModule = findBuildModule(module)
        buildModule match {
          case Some(buildModule) =>
            val moduleWithDepsAndLibsScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(buildModule, false)
            SbtModuleWithScope(buildModule, moduleWithDepsAndLibsScope)
          case None =>
            DefinitionModule(module)
        }
    }
  }

  override def findBuildModule(module: Module): Option[Module] =
    SbtBuildModuleSupport.findBuildModule(module, getProject)
}

object SbtFileImpl {

  private sealed trait TargetModule {
    def module: Module
  }

  private case object ModuleLess extends TargetModule {
    override def module: Nothing = throw new NoSuchElementException("Module not found")
  }

  private case class DefinitionModule(override val module: Module) extends TargetModule

  private case class SbtModuleWithScope(override val module: Module,
                                        moduleWithDependenciesAndLibrariesScope: GlobalSearchScope) extends TargetModule

  private def importsFor(module: TargetModule)
                        (implicit manager: ScalaPsiManager): Seq[String] = module match {
    case ModuleLess => Seq.empty
    case DefinitionModule(module) => Imports(module)
    case SbtModuleWithScope(module, moduleWithDependenciesAndLibrariesScope) =>
      val moduleScope = GlobalSearchScope.moduleScope(module)
      val localObjectsWithDefinitions = for {
        fqn <- Sbt.DefinitionHolderClasses
        aClass <- manager.getCachedClasses(moduleWithDependenciesAndLibrariesScope, fqn)

        inheritor <- ClassInheritorsSearch
          .search(aClass, moduleScope, true)
          .findAll
          .asScala
      } yield s"${inheritor.qualifiedName}._"

      Imports(module) ++ localObjectsWithDefinitions
  }
}
