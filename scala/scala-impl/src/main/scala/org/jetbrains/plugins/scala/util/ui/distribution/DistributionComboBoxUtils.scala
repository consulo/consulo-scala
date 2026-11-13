package org.jetbrains.plugins.scala.util.ui.distribution

import consulo.disposer.Disposable
import consulo.ui.ex.awt.{ComponentValidator, TextFieldWithBrowseButton, ValidationInfo}
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.extensions.PathExt

import java.nio.file.{Files, Path, StandardOpenOption}
import javax.swing.JTextField
import javax.swing.text.{AbstractDocument, AttributeSet, BadLocationException, DocumentFilter}
import scala.util.Using

/**
 * upstream: helpers for `com.intellij.openapi.roots.ui.distribution.DistributionComboBox`, which has no Consulo counterpart.
 * In Consulo the distribution is chosen with a [[TextFieldWithBrowseButton]], an empty path means "bundled".
 */
private[scala]
object DistributionComboBoxUtils {

  def installLocalDistributionInfoPointsToExistingJarFileValidator(
    field: TextFieldWithBrowseButton,
    parentDisposable: Disposable
  ): ComponentValidator = {
    val validator = new ComponentValidator(parentDisposable)
      .withValidator(() =>
        if (isSelectedPathAnExistingJarFile(field.getText.trim))
          null
        else
          new ValidationInfo(ScalaBundle.message("selected.file.is.not.a.valid.jar.file")).forComponent(field)
      )
      .withOutlineProvider(ComponentValidator.CWBB_PROVIDER)
      .andRegisterOnDocumentListener(field.getTextField)
      .installOn(field)
    validator.revalidate()
    validator
  }

  def isSelectedPathAnExistingJarFile(path: String): Boolean =
    path == null || path.isEmpty || {
      val file = Path.of(path)
      file.isRegularFile && isValidJar(file)
    }

  // Mostly adapted from `org.jetbrains.plugins.gradle.util.JarUtilKt#isValidJar`
  private def isValidJar(path: Path): Boolean =
    Using(Files.newInputStream(path, StandardOpenOption.READ)) { in =>
      val signature = new Array[Byte](2)
      in.read(signature) == 2 && signature.sameElements(Array(0x50, 0x4B))
    }.getOrElse(false)

  def setCaretToStartOnContentChange(field: TextFieldWithBrowseButton): Unit =
    setCaretToStartOnContentChange(field.getTextField)

  def setCaretToStartOnContentChange(textField: JTextField): Unit = {
    textField.getDocument match {
      case ad: AbstractDocument =>
        ad.setDocumentFilter(new DocumentFilter() {
          @throws[BadLocationException]
          override def replace(fb: DocumentFilter.FilterBypass, offset: Int, length: Int, text: String, attrs: AttributeSet): Unit = {
            super.replace(fb, offset, length, text, attrs)

            textField.setCaretPosition(0)
          }
        })
      case _ =>
    }
  }
}
