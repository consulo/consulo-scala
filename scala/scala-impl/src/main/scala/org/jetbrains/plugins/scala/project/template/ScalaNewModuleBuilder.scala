package org.jetbrains.plugins.scala.project.template

import consulo.java.language.module.extension.JavaMutableModuleExtension
import consulo.annotation.access.RequiredReadAction
import consulo.annotation.component.ExtensionImpl
import consulo.language.content.ProductionContentFolderTypeProvider
import consulo.localize.LocalizeValue
import consulo.module.content.layer.{ContentEntry, ModifiableRootModel}
import consulo.module.creation.scratch.{NewModuleBuilder, NewModuleBuilderProcessor, NewModuleContext}
import consulo.ui.ex.wizard.WizardStep
import org.jetbrains.plugins.scala.icons.Icons
import org.jetbrains.plugins.scala.project.ScalaLibraryType
import org.jetbrains.plugins.scala.project.extension.ScalaMutableModuleExtension

import consulo.virtualFileSystem.LocalFileSystem

import java.nio.file.Path
import java.util.function.Consumer

/**
 * Creates a plain Scala module: Java and Scala module extensions, the selected JDK, a module level Scala SDK library
 * and optionally the sample code.
 */
@ExtensionImpl(id = "scala", order = "after jvm")
final class ScalaNewModuleBuilder extends NewModuleBuilder {

  override def setupContext(context: NewModuleContext): Unit = {
    //noinspection ScalaExtractStringToBundle
    ScalaNewModuleGroup(context).add(LocalizeValue.localizeTODO("Empty"), Icons.SCALA_SDK, new NewModuleBuilderProcessor[ScalaNewModuleWizardContext] {
      override def createContext(isNewProject: Boolean): ScalaNewModuleWizardContext =
        new ScalaNewModuleWizardContext(isNewProject)

      override def buildSteps(consumer: Consumer[WizardStep[ScalaNewModuleWizardContext]], context: ScalaNewModuleWizardContext): Unit =
        consumer.accept(new ScalaSdkSelectStep(context))

      @RequiredReadAction
      override def process(context: ScalaNewModuleWizardContext, contentEntry: ContentEntry, modifiableRootModel: ModifiableRootModel): Unit =
        ScalaNewModuleBuilder.setupModule(context, contentEntry, modifiableRootModel)
    })
  }
}

object ScalaNewModuleBuilder {

  @RequiredReadAction
  private def setupModule(context: ScalaNewModuleWizardContext, contentEntry: ContentEntry, model: ModifiableRootModel): Unit = {
    // by id - the Java extension can come from another plugin (see JavaNewModuleBuilder)
    val javaExtension: JavaMutableModuleExtension[?] = model.getExtensionWithoutCheck("java")
    if (javaExtension != null) {
      javaExtension.setEnabled(true)
      context.sdk.foreach { sdk =>
        javaExtension.getInheritableSdk.set(null, sdk)
        model.addModuleExtensionSdkEntry(javaExtension)
      }
    }

    val scalaExtension = model.getExtensionWithoutCheck(classOf[ScalaMutableModuleExtension])
    if (scalaExtension != null) {
      scalaExtension.setEnabled(true)
    }

    context.scalaSdk.foreach { descriptor =>
      val library = model.getModuleLibraryTable.createLibrary(ScalaLibraryType.Description.libraryName(descriptor))
      val libraryModel = library.getModifiableModel
      libraryModel.setKind(ScalaLibraryType.Kind)
      //NOTE: must be called after `setKind` because it resets the properties
      libraryModel.setProperties(context.scalaSdkProperties.orNull)
      ScalaLibraryType.Description.roots(descriptor).foreach { case (url, rootType) => libraryModel.addRoot(url, rootType) }
      libraryModel.commit()
    }

    contentEntry.addFolder(contentEntry.getUrl + "/src", ProductionContentFolderTypeProvider.getInstance)

    val project = model.getProject
    IndentationSyntax.setupUseIndentationBasedSyntaxInProject(project, context.isScala3 && context.useIndentationSyntax)
    if (context.addSampleCode) {
      val root = Path.of(contentEntry.getFile.getPath)
      ScalaSampleCode.addScalaSampleCode(
        project = project,
        sourceRoot = root.resolve("src"),
        isScala3 = context.isScala3,
        useIndentationBasedSyntax = context.useIndentationSyntax,
        packagePrefix = context.packagePrefix,
        withOnboardingTips = true
      )
      LocalFileSystem.getInstance().refreshAndFindFileByNioFile(root)
    }
  }
}
