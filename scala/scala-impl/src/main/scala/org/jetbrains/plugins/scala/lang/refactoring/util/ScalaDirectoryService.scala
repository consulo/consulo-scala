package org.jetbrains.plugins.scala.lang.refactoring.util

import consulo.ide.action.ui.CreateFromTemplateDialog
import consulo.fileTemplate.{FileTemplate, FileTemplateManager, FileTemplateUtil}
import consulo.java.impl.JavaBundle
import consulo.application.ApplicationManager
import consulo.logging.Logger
import com.intellij.java.language.psi.PsiClass
import consulo.language.psi.{PsiDirectory, PsiElement}
import consulo.language.util.IncorrectOperationException
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.scala.actions.ScalaFileTemplateUtil
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

import java.util.Properties

object ScalaDirectoryService {
  private final val LOG: Logger = Logger.getInstance("#org.jetbrains.plugins.scala.lang.refactoring.move.ScalaDirectoryService")

  def createClassFromTemplate(@NotNull dir: PsiDirectory, name: String, templateName: String, askToDefineVariables: Boolean): PsiClass = {
    val templateManager = FileTemplateManager.getInstance(dir.getProject)
    // upstream: in unit test mode a FileTemplateBase built by templateForUnitTest was used (impl class is not public API in Consulo)
    val template = templateManager.getInternalTemplate(templateName)
    val defaultProperties = templateManager.getDefaultProperties
    val properties = new Properties(defaultProperties)
    properties.setProperty(FileTemplate.ATTRIBUTE_NAME, name)
    val fileName: String = name
    val element: PsiElement = try {
      if (askToDefineVariables) new CreateFromTemplateDialog(dir.getProject, dir, template, null, properties).create
      else FileTemplateUtil.createFromTemplate(template, fileName, properties, dir)
    }
    catch {
      case e: IncorrectOperationException => throw e
      case e: Exception =>
        LOG.error(e)
        return null
    }

    val file = element.getContainingFile.asInstanceOf[ScalaFile]
    file.typeDefinitions match {
      case Seq() => throw new IncorrectOperationException(getIncorrectTemplateMessage(templateName))
      case seq => seq.head
    }
  }

  private def getIncorrectTemplateMessage(templateName: String): String = {
    JavaBundle.message(
      "psi.error.incorrect.class.template.message",
      FileTemplateManager.getDefaultInstance.internalTemplateToSubject(templateName),
      templateName
    )
  }
}
