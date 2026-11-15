package org.jetbrains.plugins.scala.project.template

import consulo.application.ApplicationPropertiesComponent
import consulo.localize.LocalizeValue
import consulo.ui.CheckBox
import consulo.ui.annotation.RequiredUIAccess
import org.jetbrains.plugins.scala.ScalaBundle

/**
 * "Use significant indentation syntax" of a new Scala 3 project, shown only while a Scala 3 version is selected.
 */
trait IndentationSyntaxStepLike {

  private var indentationSyntaxCheckBox: CheckBox = scala.compiletime.uninitialized

  @RequiredUIAccess
  final protected def createIndentationSyntaxCheckBox(): CheckBox = {
    indentationSyntaxCheckBox = CheckBox.create(
      LocalizeValue.of(ScalaBundle.message("use.indentation.based.syntax.checkbox.label")),
      ApplicationPropertiesComponent.getInstance.getBoolean(IndentationSyntax.IndentationSyntaxPropertyName, false)
    )
    indentationSyntaxCheckBox.setVisible(false)
    indentationSyntaxCheckBox
  }

  @RequiredUIAccess
  final protected def setShowIndentationSyntaxCheckBox(show: Boolean): Unit =
    Option(indentationSyntaxCheckBox).foreach(_.setVisible(show))

  /**
   * Whether the project uses the indentation based syntax: the check box is shown and selected.
   */
  @RequiredUIAccess
  final protected def useIndentationSyntax: Boolean =
    Option(indentationSyntaxCheckBox).exists { checkBox =>
      val selected = checkBox.getValueOrError.booleanValue
      ApplicationPropertiesComponent.getInstance.setValue(IndentationSyntax.IndentationSyntaxPropertyName, selected, false)
      checkBox.isVisible && selected
    }
}
