package org.jetbrains.sbt.project

import consulo.annotation.access.RequiredReadAction
import consulo.annotation.component.ExtensionImpl
import consulo.fileEditor.{EditorNotificationBuilder, EditorNotificationProvider, FileEditor}
import consulo.ide.setting.ShowSettingsUtil
import consulo.java.language.module.extension.JavaModuleExtension
import consulo.language.psi.PsiManager
import consulo.language.util.ModuleUtilCore
import consulo.localize.LocalizeValue
import consulo.module.Module
import consulo.module.content.ModuleRootManager
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import jakarta.inject.Inject
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.scala.project.ModuleExt
import org.jetbrains.plugins.scala.project.notification.isScalaSourceFile
import org.jetbrains.sbt.SbtBundle
import org.jetbrains.sbt.codeInsight.daemon.SbtProjectImportStateProblemHighlightFilter

import java.util.function.Supplier

/**
 * For other examples see:
 *  - `com.intellij.codeInsight.daemon.impl.SdkSetupNotificationProvider`
 *  - `com.intellij.codeInsight.daemon.ProjectSdkSetupValidator`
 *  - `com.intellij.codeInsight.daemon.impl.JavaProjectSdkSetupValidator`
 */
@ExtensionImpl
private final class SetupScalaHighlightingNotificationProvider @Inject()(project: Project) extends EditorNotificationProvider {

  override def getId: String = "sbt-setup-scala-highlighting"

  @RequiredReadAction
  @Nullable
  override def buildNotification(file: VirtualFile, fileEditor: FileEditor, supplier: Supplier[EditorNotificationBuilder]): EditorNotificationBuilder = {
    val psiFile = PsiManager.getInstance(project).findFile(file)
    if (psiFile eq null) return null

    if (SbtProjectUtil.isInSbtProject(psiFile)) {
      // We do not track the file type of this source file, do not show any notifications.
      if (!SbtProjectImportStateProblemHighlightFilter.isTrackedFileType(file.getFileType)) return null
      // Project reload is already in progress, do not show any notifications.
      if (isSyncInProgress) return null

      // The currently open file is not a writable project source, do not show any notifications.
      // This check filters out files created from reconstructed VCS history,
      // which do not seem to belong to any IDE module and thus would return a "not imported" project state.
      // This check also filters out library source files.
      // These seem tough and could result in false positive notification banners being shown, because the project
      // could be in a very strange imported state, if the user had manually scrambled the project structure.
      // The original ticket scope also only considers writable project source files, so let's reconsider this
      // decision in the future, when it becomes necessary.
      if (!SbtProjectImportStateProblemHighlightFilter.isWritableSourceFile(psiFile)) return null

      // The project is fully imported, do not show any notifications.
      if (SbtProjectImportStateService.instance(project).isImported(psiFile)) return null

      SbtProjectImportStateNotificationPanel.fillNotification(project, supplier.get())
    } else {
      val isScalaSource = isScalaSourceFile(file, project)
      // No notification while project sync is in progress
      if (isScalaSource && !hasDeveloperKit(file) && !isSyncInProgress) createPanel(file, supplier.get())
      else null
    }
  }

  // upstream: IncompleteModelUtil.isIncompleteModel
  private def isSyncInProgress: Boolean =
    SbtProjectImportStateService.instance(project).isResolveInProgress

  private def createPanel(file: VirtualFile, builder: EditorNotificationBuilder): EditorNotificationBuilder = {
    builder.asWarning()
    builder.withText(LocalizeValue.of(SbtBundle.message("sdk.notification.provider.no.scala.sdk.in.module")))
    builder.withAction(LocalizeValue.of(SbtBundle.message("sdk.notification.provider.setup.scala.sdk")), _ => setDeveloperKit(file))
    builder
  }

  // upstream: checks for JavaModuleType - Consulo has no module types, modules with the Java module extension are checked
  private def hasDeveloperKit(file: VirtualFile): Boolean =
    findModule(file).forall { module =>
      ModuleRootManager.getInstance(module).getExtension(classOf[JavaModuleExtension[?]]) == null ||
        module.isBuildModule || // gen-idea doesn't use the sbt module type
        module.hasScala
    }

  // upstream: AddSupportForSingleFrameworkDialog for ScalaFrameworkType - Scala is enabled by the Scala module extension
  // in Consulo, so the module is selected in the project structure
  private def setDeveloperKit(file: VirtualFile): Unit =
    findModule(file).foreach { module =>
      ShowSettingsUtil.getInstance.showProjectStructureDialog(project, selector => { selector.select(module, true); () })
    }

  private def findModule(file: VirtualFile): Option[Module] =
    Option(ModuleUtilCore.findModuleForFile(file, project))
}
