package org.jetbrains.plugins.scala.actions

import consulo.annotation.component.ActionImpl
import consulo.annotation.component.ActionParentRef
import consulo.annotation.component.ActionRef
import consulo.annotation.component.ActionRefAnchor

import consulo.ide.action.CreateFromTemplateAction.moveCaretAfterNameIdentifier
import consulo.fileTemplate.AttributesDefaults
import consulo.ide.action.ui.CreateFromTemplateDialog
import consulo.fileTemplate.{FileTemplate, FileTemplateManager}
import consulo.ui.ex.action.{ActionUpdateThread, AnAction, AnActionEvent, AnActionWithSyncUpdate}
import consulo.dataContext.DataContext
import consulo.language.editor.util.IdeView
import consulo.module.Module
import com.intellij.java.language.psi.JavaDirectoryService
import consulo.language.psi.{PsiDirectory, PsiElement}
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.extensions.{ObjectExt, PsiElementExt}
import org.jetbrains.plugins.scala.icons.Icons
import org.jetbrains.plugins.scala.project._
import org.jetbrains.plugins.scala.api.project._
import org.jetbrains.plugins.scala.rt.project._
import org.jetbrains.plugins.scala.shared.project._

/**
 * @see [[NewScalaFileAction]]
 */
@ActionImpl(id = "Scala.NewPackageObject", parents = Array(new ActionParentRef(value = new ActionRef(id = "NewGroup"), anchor = ActionRefAnchor.BEFORE, relatedToAction = new ActionRef(id = "NewFile"))))
class NewPackageObjectAction extends LazyFileTemplateAction(
  "Package Object",
  ScalaBundle.message("new.packageobject.menu.action.text"),
  ScalaBundle.message("new.packageobject.menu.action.description"),
  Icons.PACKAGE_OBJECT
) with AnActionWithSyncUpdate {

  override def update(e: AnActionEvent): Unit = {
    e.getPresentation.setIcon(icon)

    val hasPackage = Option(e.getDataContext.getData(IdeView.KEY))
            .flatMap(_.getDirectories.headOption)
            .flatMap(dir => Option(JavaDirectoryService.getInstance.getPackage(dir)))
            .map(_.getQualifiedName)
            .exists(_.nonEmpty)

    val module: Module = e.getDataContext.getData(Module.KEY)
    // Don't show "Package Object" in Scala 3 projects - in Scala 3 the preferable approach is to use top-level declarations.
    // The package objects are primarily left for compatibility with Scala 2.
    // So we don't encourage users to create package objects in Scala 3 projects.
    val isEnabled: Boolean = Option(module).exists(_.hasScala2)

    e.getPresentation.setEnabled(hasPackage && isEnabled)
    e.getPresentation.setVisible(hasPackage && isEnabled)
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  override def getAttributesDefaults(dataContext: DataContext): AttributesDefaults =
    new AttributesDefaults("package").withFixedName(true)

  override def getReplacedAction(selectedTemplate: FileTemplate): AnAction = event => {
    val dataContext = event.getDataContext

    for {
      view           <- dataContext.getData(IdeView.KEY).toOption
      dir            <- getTargetDirectory(dataContext, view).toOption
      defaults        = getAttributesDefaults(dataContext)
      createdElement <- createElement(selectedTemplate, dir, defaults)
      _               = view.selectElement(createdElement)
      file           <- createdElement.containingScalaFile
      obj            <- file.typeDefinitions.headOption
    } moveCaretAfterNameIdentifier(obj)
  }

  private def createElement(
    selectedTemplate: FileTemplate,
    directory: PsiDirectory,
    defaults: AttributesDefaults
  ): Option[PsiElement] = {
    val project = directory.getProject
    FileTemplateManager.getInstance(project).addRecentName(selectedTemplate.getName)
    val properties = if (defaults != null) defaults.getDefaultProperties else null
    val dialog = new CreateFromTemplateDialog(directory, selectedTemplate, defaults, properties)
    val createdElement = dialog.create()
    ScalaFileTemplateUtil.removeBracesIfIndentationBasedSyntaxIsEnabled(project, directory, createdElement.getContainingFile)
    if (createdElement != null) {
      elementCreated(dialog, createdElement)
      Some(createdElement)
    } else None
  }
}

object NewPackageObjectAction {
  val ID: String = "Scala.NewPackageObject"
}
