package org.jetbrains.plugins.scala.util.ui.distribution

import consulo.disposer.Disposable
import consulo.ui.{HasValidator, TextBox}
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.extensions.PathExt

import java.nio.file.{Files, Path, StandardOpenOption}
import scala.util.Using

/**
 * upstream: helpers for `com.intellij.openapi.roots.ui.distribution.DistributionComboBox`, which has no Consulo counterpart.
 * In Consulo the distribution is chosen with a file chooser [[TextBox]], an empty path means "bundled".
 */
private[scala]
object DistributionComboBoxUtils {

  def installLocalDistributionInfoPointsToExistingJarFileValidator(textBox: TextBox): Disposable =
    textBox.addValidator { value =>
      if (isSelectedPathAnExistingJarFile(if (value == null) null else value.trim))
        null
      else
        new HasValidator.ValidationInfo(ScalaBundle.message("selected.file.is.not.a.valid.jar.file"))
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
}
