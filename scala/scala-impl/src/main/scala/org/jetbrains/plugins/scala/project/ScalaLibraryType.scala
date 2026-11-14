package org.jetbrains.plugins.scala.project

import consulo.annotation.component.ExtensionImpl


import org.jetbrains.plugins.scala.rt.project.Version
import consulo.application.Application
import consulo.application.progress.ProgressManager
import consulo.project.Project
import consulo.content.library.{LibraryType, NewLibraryConfiguration, PersistentLibraryKind}
import consulo.content.library.ui.{LibraryEditor, LibraryEditorComponent, LibraryPropertiesEditor}
import consulo.content.base.{BinariesOrderRootType, DocumentationOrderRootType, SourcesOrderRootType}
import consulo.disposer.Disposable
import consulo.ui.Component
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.image.Image
import consulo.virtualFileSystem.util.VirtualFileUtil
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.icons.Icons
import org.jetbrains.plugins.scala.project.external.ScalaSdkUtils

import java.nio.file.Path
import java.util.concurrent.{CancellationException, CompletableFuture}
import scala.annotation.nowarn

@ExtensionImpl
final class ScalaLibraryType extends LibraryType[ScalaLibraryProperties](ScalaLibraryType.Kind) {

  override def getIcon: Image = Icons.SCALA_SDK

  override def getCreateActionName: String = ScalaBundle.message("library.type.scala.sdk")

  override def createNewLibrary(parent: Component,
                                contextDirectory: VirtualFile,
                                project: Project): CompletableFuture[NewLibraryConfiguration] =
    ScalaLibraryType.Description.createNewLibrary(contextDirectory) match {
      case null          => CompletableFuture.failedFuture(new CancellationException())
      case configuration => CompletableFuture.completedFuture(configuration)
    }

  //noinspection TypeAnnotation
  override def createPropertiesEditor(editorComponent: LibraryEditorComponent[ScalaLibraryProperties]) =
    new LibraryPropertiesEditor {
      private var form: ScalaLibraryEditorForm = scala.compiletime.uninitialized

      @RequiredUIAccess
      override def createUIComponent(parentDisposable: Disposable): Component = {
        if (form == null) {
          form = new ScalaLibraryEditorForm(editorComponent, parentDisposable)
        }
        form.contentPanel
      }

      @RequiredUIAccess
      override def disposeUIResources(): Unit = {
        form = null
      }

      override def isModified: Boolean = form != null && formState != propertiesState

      @RequiredUIAccess
      override def apply(): Unit = {
        if (form != null && form.isValid) {
          properties.loadState(formState)
        }
      }

      @RequiredUIAccess
      override def reset(): Unit = {
        if (form == null)
          return
        val state = propertiesState
        form.languageLevel = state.getLanguageLevel
        form.classpath = state.getCompilerClasspath
        form.compilerBridgeBinaryJar = state.getCompilerBridgeBinaryJar
      }

      private def properties = editorComponent.getProperties

      private def propertiesState = properties.getState

      private def formState: ScalaLibraryPropertiesState = new ScalaLibraryPropertiesState(
        form.languageLevel,
        form.classpath,
        // NOTE: currently do not support editing scaladocExtraClasspathFrom UI, so just propagate old settings
        properties.scaladocExtraClasspath.map(ScalaLibraryProperties.fileToUrl).toArray,
        form.compilerBridgeBinaryJar.orNull,
        properties.replClasspath.asPaths.map(ScalaLibraryProperties.fileToUrl).toArray
      )
    }
}

//noinspection TypeAnnotation
object ScalaLibraryType {

  import template._

  // upstream: LibraryType.findByKind(Kind)
  def apply(): ScalaLibraryType =
    Application.get().getExtensionPoint(classOf[LibraryType[?]]).findExtensionOrFail(classOf[ScalaLibraryType])

  object Kind extends PersistentLibraryKind[ScalaLibraryProperties]("Scala") {

    override def createDefaultProperties() = ScalaLibraryProperties()
  }

  // upstream: extends CustomLibraryDescription (framework support / library options panel), Consulo has no library descriptions
  object Description {

    def createNewLibrary(contextDirectory: VirtualFile): NewLibraryConfiguration = {
      val dialogWrapper = new SdkSelectionDialogWrapper(contextDirectory)
      val selectedSdkDescriptor = dialogWrapper.open()
      val scalaLibrary = selectedSdkDescriptor.map(createNewScalaLibrary)
      scalaLibrary.orNull
    }

    // upstream: @VisibleForTesting
    private[scala] def createNewScalaLibrary(descriptor: ScalaSdkDescriptor) = {
      val ScalaSdkDescriptor(version, _, compilerClasspath, scaladocExtraClasspath, libraryFiles, sourceFiles, docFiles, compilerBridgeJar, optReplClasspath, _) = descriptor

      val compilerBridge = compilerBridgeJar.orElse {
        ProgressManager.getInstance().runProcessWithProgressSynchronously(
          () => version.flatMap(ScalaSdkUtils.resolveCompilerBridgeJar): @nowarn("msg=deprecated"),
          ScalaBundle.message("resolving.compiler.bridge.progress.message"),
          true,
          null
        )
      }

      val replClasspath = optReplClasspath.orElse {
        ProgressManager.getInstance().runProcessWithProgressSynchronously(
          () => version.map(ScalaSdkUtils.resolveReplClasspath): @nowarn("msg=deprecated"),
          ScalaBundle.message("resolving.scala.repl.classpath"),
          true,
          null
        )
      }.getOrElse(ReplClasspath.Bundled)

      new NewLibraryConfiguration(
        "scala-sdk-" + version.getOrElse(Version.Default),
        ScalaLibraryType(),
        ScalaLibraryProperties(version, compilerClasspath, scaladocExtraClasspath, compilerBridge, replClasspath)
      ) {
        override def addRoots(editor: LibraryEditor): Unit = {
          addRootsInner(libraryFiles, BinariesOrderRootType.ID)(using editor)
          addRootsInner(sourceFiles, SourcesOrderRootType.ID)(using editor)
          // upstream: JavadocOrderRootType
          addRootsInner(docFiles, DocumentationOrderRootType.ID)(using editor)

          if (sourceFiles.isEmpty && docFiles.isEmpty) editor.addRoot(
            s"https://www.scala-lang.org/api/${version.getOrElse("current")}/",
            DocumentationOrderRootType.ID
          )
        }

        private def addRootsInner(files: Iterable[Path], rootType: String)
                                 (implicit editor: LibraryEditor): Unit =
          for {
            file <- files
            url = VirtualFileUtil.getUrlForLibraryRoot(file.toFile)
          } editor.addRoot(url, rootType)
      }
    }
  }

}
