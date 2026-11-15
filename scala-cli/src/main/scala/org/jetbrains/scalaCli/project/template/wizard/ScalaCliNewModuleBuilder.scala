package org.jetbrains.scalaCli.project.template.wizard

import consulo.annotation.access.RequiredReadAction
import consulo.annotation.component.ExtensionImpl
import consulo.application.ApplicationManager
import consulo.content.base.ExcludedContentFolderTypeProvider
import consulo.localize.LocalizeValue
import consulo.module.content.layer.{ContentEntry, ModifiableRootModel}
import consulo.module.creation.scratch.{NewModuleBuilder, NewModuleBuilderProcessor, NewModuleContext}
import consulo.module.creation.ui.UnifiedProjectOrModuleNameStep
import consulo.project.startup.StartupManager
import consulo.ui.ex.wizard.WizardStep
import consulo.virtualFileSystem.LocalFileSystem
import org.jetbrains.bsp.Icons
import org.jetbrains.plugins.scala.project.template.{IndentationSyntax, ScalaNewModuleGroup, ScalaSampleCode}
import org.jetbrains.bsp.project.importing.BspOpenProjectProvider
import org.jetbrains.bsp.project.importing.BspSetupConfigStep.BspConfigSetupTask
import org.jetbrains.bsp.settings.{BspProjectSettings, PreImportConfig}
import org.jetbrains.scalaCli.ScalaCliBundle
import org.jetbrains.scalaCli.project.ScalaCliProjectUtils
import org.jetbrains.scalaCli.project.importing.ScalaCliConfigSetup

import java.nio.file.{FileAlreadyExistsException, Files, Path}
import java.util.function.Consumer

/**
 * Creates a new Scala CLI project: generates `project.scala` and optionally the sample code, generates the BSP connection file with Scala CLI
 * (`scala-cli setup-ide .`) and links the directory as a BSP project.
 */
@ExtensionImpl(id = "scala-cli", order = "after sbt")
final class ScalaCliNewModuleBuilder extends NewModuleBuilder {

  override def setupContext(context: NewModuleContext): Unit = {
    ScalaNewModuleGroup(context).add(LocalizeValue.of(ScalaCliBundle.message("scala.cli.project.system.readable.name")), Icons.BSP, ScalaNewModuleGroup.ScalaCliWeight, new NewModuleBuilderProcessor[ScalaCliNewModuleContext] {
      override def createContext(isNewProject: Boolean): ScalaCliNewModuleContext =
        new ScalaCliNewModuleContext(isNewProject)

      override def buildSteps(consumer: Consumer[WizardStep[ScalaCliNewModuleContext]], context: ScalaCliNewModuleContext): Unit = {
        consumer.accept(new ScalaCliNewModuleSettingsStep)
        consumer.accept(new UnifiedProjectOrModuleNameStep[ScalaCliNewModuleContext](context))
      }

      @RequiredReadAction
      override def process(context: ScalaCliNewModuleContext, contentEntry: ContentEntry, modifiableRootModel: ModifiableRootModel): Unit =
        ScalaCliNewModuleBuilder.setupProject(context, contentEntry, modifiableRootModel)
    })
  }
}

object ScalaCliNewModuleBuilder {

  @RequiredReadAction
  private def setupProject(context: ScalaCliNewModuleContext, contentEntry: ContentEntry, model: ModifiableRootModel): Unit = {
    val root = Path.of(contentEntry.getFile.getPath)

    if (createProjectTemplateIn(root, context.scalaVersion)) {
      // TODO consider excluding .scala-build directory
      contentEntry.addFolder(s"${contentEntry.getUrl}/.bsp", ExcludedContentFolderTypeProvider.getInstance)
    }

    val project = model.getProject
    val isScala3 = context.scalaVersion.startsWith("3.")
    IndentationSyntax.setupUseIndentationBasedSyntaxInProject(project, isScala3 && context.useIndentationSyntax)
    if (context.addSampleCode) {
      ScalaSampleCode.addScalaSampleCode(
        project = project,
        sourceRoot = root,
        isScala3 = isScala3,
        useIndentationBasedSyntax = context.useIndentationSyntax,
        packagePrefix = None,
        withOnboardingTips = false
      )
    }

    LocalFileSystem.getInstance().refreshAndFindFileByNioFile(root)

    StartupManager.getInstance(project).runWhenProjectIsInitialized { () =>
      ApplicationManager.getApplication.invokeLater { () =>
        if (!project.isDisposed) {
          // the project is linked when the BSP connection file is generated
          val task = new BspConfigSetupTask(new ScalaCliConfigSetup(root)) {
            override def onFinished(): Unit =
              if (!project.isDisposed) {
                // upstream: ScalaCliModuleBuilder.setupModule
                val settings = new BspProjectSettings
                settings.setExternalProjectPath(root.toString)
                settings.bspConfigGenerated = true
                settings.preImportConfig = PreImportConfig.NoPreImport
                new BspOpenProjectProvider().linkToExistingProject(settings, project)
              }
          }
          task.queue()
        }
      }
    }
  }

  private def createNewFile(path: Path): Boolean =
    try {
      Files.createFile(path)
      true
    } catch {
      case _: FileAlreadyExistsException => false
    }

  // upstream: ScalaCliModuleBuilder.createProjectTemplateIn
  private def createProjectTemplateIn(root: Path, scalaVersion: String): Boolean = {
    val buildFile = root.resolve(ScalaCliProjectUtils.ProjectDefinitionFileName)

    if (createNewFile(buildFile)) {
      val projectFileContent =
        s"//> using scala $scalaVersion"

      def ensureSingleNewLineAfter(text: String): String = text.stripTrailing() + "\n"

      Files.writeString(buildFile, ensureSingleNewLineAfter(projectFileContent))
      true
    }
    else false
  }
}
