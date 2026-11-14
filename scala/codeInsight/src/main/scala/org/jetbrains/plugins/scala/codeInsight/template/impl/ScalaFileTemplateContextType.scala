package org.jetbrains.plugins.scala.codeInsight.template.impl

import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.template.context.{BaseTemplateContextType, FileTypeBasedContextType, TemplateActionContext}
import consulo.language.psi.util.PsiTreeUtil
import consulo.localize.LocalizeValue
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.lang.psi.api.{ScalaFile, ScalaPsiElement}
import org.jetbrains.plugins.scala.api.{ScalaFileType, ScalaLanguage}

@ExtensionImpl
final class ScalaFileTemplateContextType extends FileTypeBasedContextType(
  "SCALA",
  ScalaLanguage.INSTANCE.getDisplayName,
  ScalaFileType.INSTANCE
) {

  override def isInContext(templateActionContext: TemplateActionContext): Boolean =
    templateActionContext.getFile.getLanguage == ScalaLanguage.INSTANCE
}

object ScalaFileTemplateContextType {

  private[impl] abstract class ElementContextType(contextId: String, @Nls presentableName: String)
    extends BaseTemplateContextType(contextId, LocalizeValue.of(presentableName), classOf[ScalaFileTemplateContextType]) {

    protected def isInContextInScalaFile(context: TemplateActionContext)(implicit file: ScalaFile): Boolean

    override final def isInContext(context: TemplateActionContext): Boolean =
      context.getFile match {
        case scalaFile: ScalaFile =>
          isInContextInScalaFile(context)(using scalaFile)
        case _ =>
          false
      }
  }

  //noinspection ConvertibleToMethodValue
  private[impl] def isInContext[T <: ScalaPsiElement](
    context: TemplateActionContext,
    clazz: Class[T]
  )(
    predicate: T => Boolean = (x: T) => true
  )(implicit file: ScalaFile): Boolean = {
    val parent = PsiTreeUtil.getParentOfType(file.findElementAt(context.getStartOffset), clazz)
    parent != null &&  predicate(parent)
  }
}
