package org.jetbrains.plugins.scala.project.template

import consulo.localize.LocalizeValue
import consulo.ui.TextBox
import consulo.ui.annotation.RequiredUIAccess
import consulo.util.lang.StringUtil
import org.jetbrains.plugins.scala.ScalaBundle

/**
 * The package of the sample code of a new project.
 */
trait PackagePrefixStepLike {

  private var packagePrefixTextBox: TextBox = scala.compiletime.uninitialized

  protected def packagePrefixLabel: LocalizeValue =
    LocalizeValue.of(ScalaBundle.message("package.prefix.label"))

  @RequiredUIAccess
  protected def createPackagePrefixTextBox(initial: Option[String]): TextBox = {
    packagePrefixTextBox = TextBox.create(initial.orNull)
    packagePrefixTextBox.setPlaceholder(LocalizeValue.of(ScalaBundle.message("package.prefix.example")))
    packagePrefixTextBox.setVisibleLength(30)
    packagePrefixTextBox
  }

  @RequiredUIAccess
  protected def packagePrefix: Option[String] =
    Option(packagePrefixTextBox).map(box => StringUtil.notNullize(box.getValue).trim).filter(_.nonEmpty)
}
