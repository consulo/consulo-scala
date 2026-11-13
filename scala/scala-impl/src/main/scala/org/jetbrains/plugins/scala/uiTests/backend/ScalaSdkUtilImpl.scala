package org.jetbrains.plugins.scala.uiTests.backend

import consulo.application.{ApplicationManager, WriteAction}
import consulo.content.library.ui.LibraryEditor
import consulo.disposer.Disposer
import consulo.module.ModuleManager
import consulo.application.progress.ProgressIndicator
import consulo.project.ProjectManager
import consulo.content.library.{Library, LibraryProperties, LibraryTable, LibraryTablesRegistrar, LibraryType, NewLibraryConfiguration, OrderRoot}
import consulo.module.content.layer.ModifiableRootModel
import consulo.module.content.util.ModuleRootModificationUtil
import org.apache.ivy.util.MessageLogger
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.scala.DependencyManagerBase.Resolver
import org.jetbrains.plugins.scala.components.libextensions.ProgressIndicatorLogger
import org.jetbrains.plugins.scala.extensions.withProgressSynchronouslyTry
import org.jetbrains.plugins.scala.project.ScalaLibraryType
import org.jetbrains.plugins.scala.project.template.ScalaVersionDownloadingDialog.{ScalaVersionResolveResult, createScalaVersionResolveResult}
import org.jetbrains.plugins.scala.project.template.{Artifact, ScalaSdkDescriptor}
import org.jetbrains.plugins.scala.{DependencyManagerBase, ScalaBundle, ScalaVersion}

import consulo.virtualFileSystem.VirtualFile

import java.util
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Try

private[backend] object ScalaSdkUtilImpl {
  def setupScalaSdk(@NotNull scalaVersionString: String): Unit = {
    val project = ProjectManager.getInstance.getOpenProjects()(0)
    val module = ModuleManager.getInstance(project).getModules()(0)

    val newLibraryConfiguration = createNewScalaLibraryConfiguration(scalaVersionString)
    ModuleRootModificationUtil.updateModel(module, (model: ModifiableRootModel) => {
      addProjectLibrary(model, newLibraryConfiguration)
    })

    // upstream: StoreUtil.saveSettings(project, true)
    ApplicationManager.getApplication.saveAllWithProgress(project.getUIAccess).join()
  }

  private def createNewScalaLibraryConfiguration(scalaVersionString: String): NewLibraryConfiguration = {
    val scalaVersion = ScalaVersion.fromString(scalaVersionString).getOrElse {
      throw new IllegalArgumentException(s"Invalid scala version: $scalaVersionString")
    }
    val resolvedScalaVersion = tryDownloadScalaWithProgress(scalaVersion).get
    val scalaSdkDescriptor = convertScalaResolveResultToScalaSdkDescriptor(resolvedScalaVersion)

    ScalaLibraryType.Description.createNewScalaLibrary(scalaSdkDescriptor)
  }

  //Copied from:
  // com.intellij.testFramework.PsiTestUtil.addProjectLibrary(com.intellij.openapi.roots.ModifiableRootModel, java.lang.String, java.util.List<? extends com.intellij.openapi.vfs.VirtualFile>, java.util.List<? extends com.intellij.openapi.vfs.VirtualFile>, java.util.List<? extends com.intellij.openapi.vfs.VirtualFile>, java.util.List<? extends com.intellij.openapi.vfs.VirtualFile>)
  private def addProjectLibrary(
    model: ModifiableRootModel,
    libraryConfiguration: NewLibraryConfiguration,
  ): Library = {
    WriteAction.computeAndWait(() => {
      val libraryTableModel: LibraryTable.ModifiableModel = model.getModuleLibraryTable.getModifiableModel

      val projectLibraryTable: LibraryTable = LibraryTablesRegistrar.getInstance.getLibraryTable(model.getProject)

      val libraryName = suggestNewLibraryName(libraryTableModel, libraryConfiguration.getDefaultLibraryName)
      val library: Library = projectLibraryTable.createLibrary(libraryName)
      val libraryModel = library.getModifiableModel.asInstanceOf[Library.ModifiableModel]

      try {
        //NOTE: the red code comes from SCL-23078
        val libraryType: LibraryType[?] = libraryConfiguration.getLibraryType
        val libraryKind = if (libraryType != null) libraryType.getKind else null
        libraryModel.setKind(libraryKind)
        if (libraryConfiguration.getProperties != null)
          libraryModel.setProperties(libraryConfiguration.getProperties)

        // upstream: NewLibraryEditor + applyTo(libraryModel)
        libraryConfiguration.addRoots(new ModifiableModelLibraryEditor(libraryModel, libraryType))
      } catch {
        case t: Throwable =>
          // upstream: libraryTableModel.dispose()
          Disposer.dispose(libraryModel)
          throw t
      }

      libraryModel.commit()
      libraryTableModel.commit()

      model.addLibraryEntry(library)

      library
    })
  }

  // upstream: LibraryEditingUtil.suggestNewLibraryName
  private def suggestNewLibraryName(table: LibraryTable.ModifiableModel, baseName: String): String = {
    var candidateName = baseName
    var idx = 1
    while (table.getLibraryByName(candidateName) != null) {
      candidateName = s"$baseName ($idx)"
      idx += 1
    }
    candidateName
  }

  private final class ModifiableModelLibraryEditor(model: Library.ModifiableModel, libraryType: LibraryType[?]) extends LibraryEditor {
    override def getName: String = model.getName
    override def getUrls(rootType: String): Array[String] = model.getUrls(rootType)
    override def getFiles(rootType: String): Array[VirtualFile] = model.getFiles(rootType)
    override def getExcludedRootUrls: Array[String] = model.getExcludedRootUrls
    override def setName(name: String): Unit = model.setName(name)
    override def addRoot(file: VirtualFile, rootType: String): Unit = model.addRoot(file, rootType)
    override def addRoot(url: String, rootType: String): Unit = model.addRoot(url, rootType)
    override def addJarDirectory(file: VirtualFile, recursive: Boolean, rootType: String): Unit = model.addJarDirectory(file, recursive, rootType)
    override def addJarDirectory(url: String, recursive: Boolean, rootType: String): Unit = model.addJarDirectory(url, recursive, rootType)
    override def addExcludedRoot(url: String): Unit = model.addExcludedRoot(url)
    override def removeRoot(url: String, rootType: String): Unit = model.removeRoot(url, rootType)
    override def removeExcludedRoot(url: String): Unit = model.removeExcludedRoot(url)
    override def removeAllRoots(): Unit = ()
    override def hasChanges: Boolean = model.isChanged
    override def isJarDirectory(url: String, rootType: String): Boolean = model.isJarDirectory(url, rootType)
    override def isValid(url: String, orderRootType: String): Boolean = model.isValid(url, orderRootType)
    override def getProperties: LibraryProperties[?] = model.getProperties
    override def getType: LibraryType[?] = libraryType
    override def addRoots(roots: util.Collection[? <: OrderRoot]): Unit =
      roots.asScala.foreach { root =>
        if (root.isJarDirectory) model.addJarDirectory(root.getFile, false, root.getType)
        else model.addRoot(root.getFile, root.getType)
      }
  }

  private def tryDownloadScalaWithProgress(scalaVersion: ScalaVersion): Try[ScalaVersionResolveResult] = {
    withProgressSynchronouslyTry(ScalaBundle.message("downloading.scala.version", scalaVersion.minor), canBeCanceled = true) { manager =>
      val indicator = manager.getProgressIndicator
      val dependencyManager = new DependencyManagerBase {
        override protected def progressIndicator: Option[ProgressIndicator] = Some(indicator)
        override def createLogger: MessageLogger = new ProgressIndicatorLogger(indicator)
        override def resolvers: Seq[DependencyManagerBase.Resolver] = Seq(Resolver.JetBrainsMavenCentralMirror)
      }
      createScalaVersionResolveResult(scalaVersion, dependencyManager)
    }
  }

  private val ScalaLibraryFileNames = Artifact.ScalaLibraryAndModulesArtifacts.map(_.prefix)

  private def convertScalaResolveResultToScalaSdkDescriptor(scalaVersionResolveResult: ScalaVersionResolveResult): ScalaSdkDescriptor = {
    val compilerJars = scalaVersionResolveResult.compilerClassPathJars
    val libraryJars = compilerJars.filter(f => ScalaLibraryFileNames.exists(f.getFileName.toString.startsWith(_)))
    val scaladocExtraClasspath = Nil // TODO SCL-17219
    ScalaSdkDescriptor(
      version = Some(scalaVersionResolveResult.scalaVersion),
      label = None,
      compilerClasspath = compilerJars,
      scaladocExtraClasspath = scaladocExtraClasspath,
      libraryFiles = libraryJars,
      sourceFiles = scalaVersionResolveResult.librarySourcesJars,
      docFiles = Nil, // docs are not downloaded
      compilerBridgeJar = scalaVersionResolveResult.compilerBridgeJar,
      replClasspath = None
    )
  }
}
