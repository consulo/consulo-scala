package org.jetbrains.sbt.project.template

import consulo.annotation.access.RequiredReadAction
import consulo.annotation.component.ExtensionImpl
import consulo.application.ApplicationManager
import consulo.content.base.ExcludedContentFolderTypeProvider
import consulo.language.content.{ProductionContentFolderTypeProvider, TestContentFolderTypeProvider}
import consulo.localize.LocalizeValue
import consulo.module.content.layer.{ContentEntry, ModifiableRootModel}
import consulo.module.creation.scratch.{NewModuleBuilder, NewModuleBuilderProcessor, NewModuleContext}
import consulo.module.creation.ui.UnifiedProjectOrModuleNameStep
import consulo.project.startup.StartupManager
import consulo.ui.ex.wizard.WizardStep
import consulo.virtualFileSystem.LocalFileSystem
import org.jetbrains.plugins.scala.project.template.{DefaultModuleContentEntryFolders, IndentationSyntax, ScalaNewModuleGroup, ScalaSampleCode}
import org.jetbrains.sbt.Sbt
import org.jetbrains.sbt.project.SbtOpenProjectProvider

import java.nio.file.{Files, Path}
import java.util.function.Consumer
import scala.util.Using

/**
 * Creates a new sbt project: generates `build.sbt`, `project/build.properties`, the source directories and optionally
 * the sample code, then links the directory as an sbt project, which is imported by the sbt external system.
 */
@ExtensionImpl(id = "sbt", order = "after jvm")
final class SbtNewModuleBuilder extends NewModuleBuilder {

  override def setupContext(context: NewModuleContext): Unit = {
    ScalaNewModuleGroup(context).add(LocalizeValue.of(Sbt.Name), Sbt.Icon, new NewModuleBuilderProcessor[SbtNewModuleContext] {
      override def createContext(isNewProject: Boolean): SbtNewModuleContext =
        new SbtNewModuleContext(isNewProject)

      override def buildSteps(consumer: Consumer[WizardStep[SbtNewModuleContext]], context: SbtNewModuleContext): Unit = {
        consumer.accept(new SbtNewModuleSettingsStep(context))
        consumer.accept(new UnifiedProjectOrModuleNameStep[SbtNewModuleContext](context))
      }

      @RequiredReadAction
      override def process(context: SbtNewModuleContext, contentEntry: ContentEntry, modifiableRootModel: ModifiableRootModel): Unit =
        SbtNewModuleBuilder.setupProject(context, contentEntry, modifiableRootModel)
    })
  }
}

object SbtNewModuleBuilder {

  @RequiredReadAction
  private def setupProject(context: SbtNewModuleContext, contentEntry: ContentEntry, model: ModifiableRootModel): Unit = {
    val root = Path.of(contentEntry.getFile.getPath)

    val folders = SbtModuleBuilder.createProjectTemplateIn(
      root = root,
      name = context.getName,
      scalaVersion = context.scalaVersion,
      sbtVersion = context.sbtVersion,
      packagePrefix = None,
      addLibraryDependenciesExample = context.sbtVersion.isSbt2
    )
    if (context.isNewProject) {
      writeGitIgnore(root)
    }
    folders.foreach(addContentFolders(contentEntry, _))

    val project = model.getProject
    val isScala3 = context.scalaVersion.startsWith("3.")
    IndentationSyntax.setupUseIndentationBasedSyntaxInProject(project, isScala3 && context.useIndentationSyntax)
    if (context.addSampleCode) {
      ScalaSampleCode.addScalaSampleCode(
        project = project,
        sourceRoot = root.resolve("src/main/scala"),
        isScala3 = isScala3,
        useIndentationBasedSyntax = context.useIndentationSyntax,
        packagePrefix = context.packagePrefix,
        withOnboardingTips = true
      )
    }

    LocalFileSystem.getInstance().refreshAndFindFileByNioFile(root)

    val externalProjectPath = root.toString
    StartupManager.getInstance(project).runWhenProjectIsInitialized { () =>
      ApplicationManager.getApplication.invokeLater { () =>
        if (!project.isDisposed) {
          new SbtOpenProjectProvider().linkToExistingProject(externalProjectPath, project, { settings =>
            settings.resolveClassifiers = context.downloadScalaSdkSources
            settings.resolveSbtClassifiers = context.downloadSbtSources
          })
        }
      }
    }
  }

  private def addContentFolders(contentEntry: ContentEntry, folders: DefaultModuleContentEntryFolders): Unit = {
    val url = contentEntry.getUrl
    folders.sources.foreach(path => contentEntry.addFolder(s"$url/$path", ProductionContentFolderTypeProvider.getInstance))
    folders.testSources.foreach(path => contentEntry.addFolder(s"$url/$path", TestContentFolderTypeProvider.getInstance))
    folders.excluded.foreach(path => contentEntry.addFolder(s"$url/$path", ExcludedContentFolderTypeProvider.getInstance))
  }

  private def writeGitIgnore(root: Path): Unit = {
    val target = root.resolve(".gitignore")
    Option(getClass.getResourceAsStream("/assets/ignore/scala-gitignore.txt")).filterNot(_ => Files.exists(target)).foreach { stream =>
      Using.resource(stream)(Files.copy(_, target))
    }
  }
}
