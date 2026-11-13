package org.jetbrains.plugins.scala.project.template

import com.intellij.facet.impl.ui.libraries.LibraryCompositionSettings
import com.intellij.ide.util.projectWizard.JavaModuleBuilder
import consulo.module.Module
import consulo.project.Project
import consulo.module.content.layer.ModifiableRootModel
import consulo.content.library.Library
import consulo.virtualFileSystem.VirtualFile

import java.{util => ju}

class ScalaModuleBuilder extends JavaModuleBuilder {

  var libraryCompositionSettings: LibraryCompositionSettings = scala.compiletime.uninitialized
  var packagePrefix = Option.empty[String]
  var openFileEditorAfterProjectOpened: Seq[VirtualFile] = Nil

  locally {
    addModuleConfigurationUpdater((_: Module, rootModel: ModifiableRootModel) => {
      val mutableEmptyList = new ju.ArrayList[Library]()
      libraryCompositionSettings.addLibraries(rootModel, mutableEmptyList, null)
      packagePrefix.foreach(prefix => rootModel.getContentEntries.foreach(_.getSourceFolders.foreach(_.setPackagePrefix(prefix))))
    })
  }

  override def setupModule(module: Module): Unit = {
    //execute when current dialog is closed
    openEditorForCodeSample(module.getProject)
    super.setupModule(module)
  }

  //open code sample or buildSbt
  private def openEditorForCodeSample(project: Project): Unit =
    ModuleBuilderUtil.openFilesInEditor(openFileEditorAfterProjectOpened, project)
}
