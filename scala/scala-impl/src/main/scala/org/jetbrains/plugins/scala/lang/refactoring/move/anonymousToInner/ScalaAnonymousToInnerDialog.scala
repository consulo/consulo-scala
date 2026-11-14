package org.jetbrains.plugins.scala.lang.refactoring.move.anonymousToInner

import consulo.java.impl.refactoring.JavaRefactoringBundle
import consulo.project.Project
import consulo.ui.ex.awt.DialogWrapper
import consulo.language.psi.PsiManager
import com.intellij.java.language.psi.PsiNameHelper
import com.intellij.java.impl.refactoring.HelpID
import consulo.language.editor.refactoring.ui.NameSuggestionsField
import consulo.language.editor.refactoring.util.CommonRefactoringUtil
import com.intellij.java.impl.refactoring.util.{ParameterTablePanel, RefactoringMessageUtil}
import com.intellij.java.analysis.impl.refactoring.util.VariableData
import consulo.localize.LocalizeValue
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.ex.awtUnsafe.TargetAWT
import consulo.ui.layout.LabeledLayout
import consulo.ui.util.FormBuilder
import org.jetbrains.annotations.{Nls, TestOnly}
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.api.lang.psi.api.ScFile
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.templates.ScExtendsBlock
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTemplateDefinition
import org.jetbrains.plugins.scala.lang.psi.types.ScParameterizedType
import org.jetbrains.plugins.scala.lang.psi.types.api.designator.ScDesignatorType
import org.jetbrains.plugins.scala.lang.refactoring.extractMethod.ScalaVariableData

import javax.swing.JComponent
import scala.jdk.CollectionConverters._

/**
 * @note original implementation was inspired by [[com.intellij.refactoring.anonymousToInner.AnonymousToInnerDialog]]
 */
class ScalaAnonymousToInnerDialog(
  project: Project,
  extendsBlock: ScExtendsBlock,
  _variables: Seq[ScalaVariableData],
  target: Either[ScFile, ScTemplateDefinition]
) extends DialogWrapper(project) {

  //This modifiable array is modified directly by `ParameterTablePanel`
  //NOTE: it's not covered by tests, because it implies some UI interaction
  private val variablesModifiableModel: Array[ScalaVariableData] = _variables.toArray

  setTitle(ScalaBundle.message("move.anonymousToInner.dialog.title"))

  private val classNameField: NameSuggestionsField = new NameSuggestionsField(project)
  classNameField.setSuggestions(suggestNewClassNames())
  classNameField.selectNameWithoutExtension()

  init()

  private def suggestNewClassNames(): Array[String] =
    extendsBlock.superTypes.head match {
      case pType: ScParameterizedType =>
        val name = pType.designator.toString
        val typeParameters = pType.typeArguments.map(_.toString).mkString + name
        Array(typeParameters, "My" + name)
      case designator: ScDesignatorType =>
        val name = designator.toString
        Array("My" + name)
    }

  override protected def doOKAction(): Unit = {
    @Nls var errorString: String = null
    val innerClassName = getClassName
    val manager = PsiManager.getInstance(project)
    if (innerClassName.isEmpty) errorString = JavaRefactoringBundle.message("anonymousToInner.no.inner.class.name")
    else {
      if (!PsiNameHelper.getInstance(manager.getProject).isIdentifier(innerClassName)) {
        errorString = RefactoringMessageUtil.getIncorrectIdentifierMessage(innerClassName)
      }
      else {
        val existingTemplateDefinitionNames: Set[String] = target.fold(
          file =>
            file.getClassNames.asScala.toSet,
          template =>
            template.extendsBlock
              .getOrCreateTemplateBody
              .getChildren
              .collect { case c: ScTemplateDefinition => c.name }
              .toSet
        )

        if (existingTemplateDefinitionNames.contains(innerClassName)) {
          errorString = JavaRefactoringBundle.message("inner.class.exists", innerClassName, target.map(_.name).getOrElse("TOP LEVEL DEFINTION"))
        }
      }
    }

    if (errorString != null) {
      CommonRefactoringUtil.showErrorMessage(ScalaAnonymousToInnerHandler.RefactoringTitle, errorString, HelpID.ANONYMOUS_TO_INNER, project)
    } else {
      super.doOKAction()
    }
    classNameField.requestFocusInWindow
  }

  @RequiredUIAccess
  override protected def createNorthPanel: JComponent = {
    val form = FormBuilder.create()
      .addLabeled(LocalizeValue.of(JavaRefactoringBundle.message("anonymousToInner.class.name.label.text")), TargetAWT.wrap(classNameField))
      .build()
    TargetAWT.to(form).asInstanceOf[JComponent]
  }

  override def getPreferredFocusedComponent: JComponent = classNameField.getFocusableComponent

  @RequiredUIAccess
  override def createCenterPanel(): JComponent = {
    val panel = LabeledLayout.create(
      LocalizeValue.of(JavaRefactoringBundle.message("anonymousToInner.parameters.panel.border.title")),
      TargetAWT.wrap(createParametersPanel)
    )
    TargetAWT.to(panel).asInstanceOf[JComponent]
  }

  def getClassName: String = classNameField.getEnteredName
  @TestOnly
  def setClassName(name: String): Unit = classNameField.setName(name)

  def getVariables: Seq[ScalaVariableData] =
    variablesModifiableModel.filter(_.passAsParameter).toSeq

  private def createParametersPanel: ParameterTablePanel = {
    val panel = new ParameterTablePanel(project, variablesModifiableModel.asInstanceOf[Array[VariableData]], extendsBlock) {
      override protected def updateSignature(): Unit = {
      }

      override protected def doEnterAction(): Unit = {
        clickDefaultButton()
      }

      override protected def doCancelAction(): Unit = {
        this.asInstanceOf[DialogWrapper].doCancelAction()
      }
    }
    panel
  }

  override protected def getHelpId: String = HelpID.ANONYMOUS_TO_INNER
}
