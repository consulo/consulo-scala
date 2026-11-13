package org.jetbrains.plugins.scala.actions

import consulo.ide.action.CreateFromTemplateActionBase
import consulo.fileTemplate.{FileTemplate, FileTemplateManager}
import consulo.application.dumb.DumbAware
import consulo.project.Project
import consulo.language.psi.PsiDirectory
import org.jetbrains.annotations.Nls

import javax.swing.Icon

/**
 * @param templateName name of the file template<br>
 *                     It's defined in the plugin.xml file with the tag {{{<internalFileTemplate ... />}}}
 *                     The actual template file full name includes extension `.scala.ft,`
 *                     For example, `Scala CaseObject.scala.ft`.
 */
abstract class LazyFileTemplateAction(
  templateName: String,
  @Nls title: String,
  @Nls description: String,
  val icon: Icon
) extends CreateFromTemplateActionBase(
  title,
  description,
  icon
) with DumbAware {

  private lazy val template = FileTemplateManager.getDefaultInstance.getInternalTemplate(templateName)

  override def getTemplate(project: Project, dir: PsiDirectory): FileTemplate = template
}
