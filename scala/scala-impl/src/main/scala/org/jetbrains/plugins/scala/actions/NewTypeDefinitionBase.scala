package org.jetbrains.plugins.scala.actions

import consulo.ui.image.Image
import consulo.ide.action.CreateTemplateInPackageAction
import consulo.fileTemplate.FileTemplateManager
import com.intellij.java.language.impl.codeInsight.template.JavaTemplateUtil
import consulo.util.lang.ControlFlowException
import consulo.virtualFileSystem.fileType.FileType
import consulo.project.Project
import com.intellij.java.language.psi.JavaDirectoryService
import consulo.language.psi.{PsiDirectory, PsiElement, PsiFile, PsiFileFactory}
import consulo.language.codeStyle.CodeStyleManager
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTemplateDefinition

import java.util.Properties

abstract class NewTypeDefinitionBase[T <: ScTemplateDefinition](@Nls txt: String, @Nls description: String, icon: Image)
  extends CreateTemplateInPackageAction[T](txt, description, icon, true) {

  override def checkPackageExists(psiDirectory: PsiDirectory): Boolean = JavaDirectoryService.getInstance.getPackage(psiDirectory) != null

  override def getNavigationElement(t: T): PsiElement = t.extendsBlock

  def createFromTemplate(directory: PsiDirectory, name: String, fileName: String, templateName: String,
                         parameters: String*): PsiFile = {
    val templateManager = FileTemplateManager.getDefaultInstance
    val template = templateManager.getInternalTemplate(templateName)
    val project = directory.getProject
    val properties = new Properties(templateManager.getDefaultProperties)

    JavaTemplateUtil.setPackageNameAttribute(properties, directory)
    properties.setProperty(NewTypeDefinitionBase.NAME_TEMPLATE_PROPERTY, name)
    properties.setProperty(NewTypeDefinitionBase.LOW_CASE_NAME_TEMPLATE_PROPERTY, name.substring(0, 1).toLowerCase + name.substring(1))

    for (j <- 0.until(parameters.length, 2)) {
      properties.setProperty(parameters(j), parameters(j + 1))
    }

    var text: String = null

    try text = template.getText(properties) catch {
      case c: ControlFlowException => throw c
      case e: Exception =>
        throw new RuntimeException("Unable to load template for " + templateManager.internalTemplateToSubject(templateName), e)
    }

    val file = createFile(fileName, text, project)
    CodeStyleManager.getInstance(project).reformat(file)
    directory.add(file).asInstanceOf[PsiFile]
  }

  protected def getFileType: FileType

  protected def createFile(name: String, text: String, project: Project): PsiFile =
    PsiFileFactory.getInstance(project).createFileFromText(name, getFileType, text)
}

object NewTypeDefinitionBase {
  val NAME_TEMPLATE_PROPERTY: String = "NAME"
  val LOW_CASE_NAME_TEMPLATE_PROPERTY: String = "lowCaseName"
}
