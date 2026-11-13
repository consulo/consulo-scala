package org.jetbrains.plugins.scala.codeInspection.packageNameInspection

import consulo.application.CommonBundle
import consulo.module.Module
import consulo.project.Project
import consulo.module.content.ModuleRootManager
import com.intellij.openapi.roots.SourceFolder
import consulo.ui.ex.awt.Messages
import com.intellij.refactoring.util.CommonMoveClassesOrPackagesUtil
import com.intellij.java.impl.refactoring.util.RefactoringMessageUtil
import consulo.language.util.IncorrectOperationException
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.codeInspection.{AbstractFixOnPsiElement, ScalaInspectionBundle}
import org.jetbrains.plugins.scala.codeInspection.packageNameInspection.ScalaMoveToPackageQuickFix._
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.project.ProjectPsiElementExt

final class ScalaMoveToPackageQuickFix(_file: ScalaFile, packageName: String)
      extends AbstractFixOnPsiElement(ScalaMoveToPackageQuickFix.hint(packageName), _file) {

  override protected def doApplyFix(file: ScalaFile)
                                   (implicit project: Project): Unit = {
    val directory = try {
      // upstream: checks that the package name is compatible with the package prefix of the source root,
      // Consulo source roots have no package prefixes
      val directory = CommonMoveClassesOrPackagesUtil.chooseDestinationPackage(
        project,
        packageName.replace("`", ""), // ugly hack to support most escaped package names... if they contain a dot... well, good night!
        file.getContainingDirectory
      )
      if (directory == null) {
        return
      }

      RefactoringMessageUtil.checkCanCreateFile(directory, file.name) match {
        case null => directory
        case error => throw new IncorrectOperationException(error)
      }
    } catch {
      case e: IncorrectOperationException =>
        Messages.showMessageDialog(project, e.getLocalizedMessage, CommonBundle.getErrorTitle, Messages.getErrorIcon)
        return
    }

    inWriteAction {
      file.getVirtualFile.move(project, directory.getVirtualFile)
    }
  }

  override def startInWriteAction(): Boolean = false

  override def getFamilyName: String = ScalaInspectionBundle.message("fimaly.name.move.file.to.package")
}

private object ScalaMoveToPackageQuickFix {
  @Nls
  def hint(packageName: String): String = {
    if (packageName.isEmpty) ScalaInspectionBundle.message("move.file.to.default.package")
    else ScalaInspectionBundle.message("move.file.to.package.with.packagename", s"'$packageName'")
  }

}