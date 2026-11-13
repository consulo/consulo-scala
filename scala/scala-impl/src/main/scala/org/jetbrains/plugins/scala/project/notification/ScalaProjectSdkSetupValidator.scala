package org.jetbrains.plugins.scala.project.notification

import com.intellij.java.language.JavaCoreBundle
import consulo.annotation.access.RequiredReadAction
import consulo.annotation.component.ExtensionImpl
import consulo.fileEditor.{EditorNotificationBuilder, EditorNotificationProvider, FileEditor}
import consulo.ide.setting.ShowSettingsUtil
import consulo.java.language.module.extension.JavaModuleExtension
import consulo.language.psi.PsiManager
import consulo.language.util.ModuleUtilCore
import consulo.localize.LocalizeValue
import consulo.module.content.ModuleRootManager
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import jakarta.inject.Inject

import java.util.function.Supplier
import scala.jdk.CollectionConverters.ListHasAsScala

/**
 * upstream: ProjectSdkSetupValidator delegating to JavaProjectSdkSetupValidator,
 * Consulo shows the notification via [[EditorNotificationProvider]] (see consulo-java SetupJDKNotificationProvider)
 */
@ExtensionImpl
final class ScalaProjectSdkSetupValidator @Inject()(project: Project) extends EditorNotificationProvider {

  override def getId: String = "scala-sdk-notify"

  @RequiredReadAction
  override def buildNotification(file: VirtualFile, fileEditor: FileEditor, supplier: Supplier[EditorNotificationBuilder]): EditorNotificationBuilder = {
    if (!isScalaSourceFile(file, project))
      return null

    val psiFile = PsiManager.getInstance(project).findFile(file)
    val module = ModuleUtilCore.findModuleForPsiElement(psiFile)
    if (module == null)
      return null

    val extension = ModuleRootManager.getInstance(module).getExtensions.asScala.collectFirst { case e: JavaModuleExtension[?] => e }
    if (extension.forall(!_.getInheritableSdk.isNull))
      return null

    val builder = supplier.get()
    builder.withText(LocalizeValue.of(JavaCoreBundle.message("module.jdk.not.defined")))
    builder.withAction(LocalizeValue.of(JavaCoreBundle.message("module.jdk.setup")), _ =>
      ShowSettingsUtil.getInstance.showProjectStructureDialog(project, selector => { selector.select(module, true); () })
    )
    builder
  }
}
