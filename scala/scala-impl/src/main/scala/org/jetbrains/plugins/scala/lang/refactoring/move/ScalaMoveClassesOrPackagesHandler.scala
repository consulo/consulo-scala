package org.jetbrains.plugins.scala.lang.refactoring.move

import consulo.project.Project
import consulo.ui.ex.awt.{DialogWrapper, Messages}
import com.intellij.java.language.psi.PsiClass
import consulo.language.psi.{PsiDirectory, PsiElement, PsiPackage}
import consulo.language.editor.refactoring.move.MoveCallback
import com.intellij.java.impl.refactoring.move.moveClassesOrPackages.{JavaMoveClassesOrPackagesHandler, MoveClassesOrPackagesDialog, MoveClassesOrPackagesImpl, MoveClassesOrPackagesProcessor, MoveClassesOrPackagesToNewDirectoryDialog}
import consulo.language.editor.refactoring.util.{CommonRefactoringUtil, TextOccurrencesUtil}
import com.intellij.java.impl.refactoring.{HelpID, JavaRefactoringSettings, MoveDestination}
import org.jetbrains.annotations.{NotNull, Nullable}
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaFileImpl
import org.jetbrains.plugins.scala.settings.ScalaApplicationSettings

import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.{util => ju}
import javax.swing._
import scala.annotation.nowarn

final class ScalaMoveClassesOrPackagesHandler extends JavaMoveClassesOrPackagesHandler {
  override def doMove(project: Project, elements: Array[PsiElement], targetContainer: PsiElement, callback: MoveCallback): Unit = {
    def refactoringIsNotSupported(): Unit = {
      Messages.showErrorDialog(ScalaBundle.message("move.to.inner.is.not.supported"), ScalaBundle.message("move.to.inner.is.not.supported.title"))
    }
    targetContainer match {
      case _: ScTypeDefinition =>
        refactoringIsNotSupported()
        return
      case _: PsiClass =>
        if (elements.exists(_.isInstanceOf[ScTypeDefinition])) {
          refactoringIsNotSupported()
          return
        }
      case _ =>
    }
    ScalaFileImpl.performMoveRefactoring {
      doMoveImpl(project, elements, targetContainer, callback)
    }
  }

  // upstream: JavaMoveClassesOrPackagesHandler.doMove called the doMoveWithMoveClassesDialog and
  // createMoveClassesOrPackagesToNewDirectoryDialog hooks, which Consulo's handler doesn't have.
  // Directory/package moves are delegated to the Java handler, class moves use the Scala dialogs.
  private def doMoveImpl(project: Project, elements: Array[PsiElement], targetContainer: PsiElement, callback: MoveCallback): Unit = {
    if (elements.exists(e => e.isInstanceOf[PsiDirectory] || e.isInstanceOf[PsiPackage])) {
      super.doMove(project, elements, targetContainer, callback)
      return
    }
    val adjustedElements = MoveClassesOrPackagesImpl.adjustForMove(project, elements, targetContainer)
    if (adjustedElements == null) return

    targetContainer match {
      case targetDirectory: PsiDirectory
        if CommonRefactoringUtil.checkReadOnlyStatusRecursively(project, ju.Arrays.asList(adjustedElements*), true) &&
          !JavaMoveClassesOrPackagesHandler.packageHasMultipleDirectoriesInModule(project, targetDirectory) =>
        createMoveClassesOrPackagesToNewDirectoryDialog(targetDirectory, adjustedElements, callback).show()
      case _ =>
        doMoveWithMoveClassesDialog(project, adjustedElements, targetContainer, callback)
    }
  }

  override def canMove(elements: Array[PsiElement], targetContainer: PsiElement): Boolean = {
    //sort of hack to save destinations here, need to be sure that it is called
    val scalaElements = elements.filter(_.getLanguage.isKindOf(ScalaLanguage.INSTANCE))
    targetContainer match {
      case dir: PsiDirectory => scalaElements.foreach(saveMoveDestination(_, dir))
      case _ =>
    }
    elements.length == scalaElements.length && (super.canMove(elements, targetContainer): @nowarn("cat=deprecation"))
  }

  private def doMoveWithMoveClassesDialog(project: Project,
                                                     adjustedElements: Array[PsiElement],
                                                     initialTargetElement: PsiElement,
                                                     moveCallback: MoveCallback): Unit = {
    if (!CommonRefactoringUtil.checkReadOnlyStatusRecursively(project, ju.Arrays.asList(adjustedElements*), true)) {
      return
    }
    val initialTargetPackageName: String = MoveClassesOrPackagesImpl.getInitialTargetPackageName(initialTargetElement, adjustedElements)
    val initialTargetDirectory: PsiDirectory = MoveClassesOrPackagesImpl.getInitialTargetDirectory(initialTargetElement, adjustedElements)
    val searchTextOccurences: Boolean = adjustedElements.exists(TextOccurrencesUtil.isSearchTextOccurencesEnabled)
    val searchInComments: Boolean = JavaRefactoringSettings.getInstance.MOVE_SEARCH_IN_COMMENTS
    val searchForTextOccurences: Boolean = JavaRefactoringSettings.getInstance.MOVE_SEARCH_FOR_TEXT

    val moveDialog: MoveClassesOrPackagesDialog =
      new MoveClassesOrPackagesDialog(
        project,
        searchTextOccurences,
        adjustedElements,
        initialTargetElement,
        moveCallback) {

        override def createCenterPanel(): JComponent = {
          addMoveCompanionChb(super.createCenterPanel(), adjustedElements)
        }

        override def createMoveToPackageProcessor(destination: MoveDestination,
                                                  elementsToMove: Array[PsiElement],
                                                  callback: MoveCallback): MoveClassesOrPackagesProcessor = {

          new MoveClassesOrPackagesProcessor(project, elementsToMove, destination, isSearchInComments, searchTextOccurences, callback)
        }
      }

    moveDialog.setData(
      adjustedElements,
      initialTargetPackageName,
      initialTargetDirectory,
      initialTargetDirectory == null,
      initialTargetElement == null,
      searchInComments,
      searchForTextOccurences,
      HelpID.getMoveHelpID(adjustedElements(0)))

    ScalaFileImpl.performMoveRefactoring {
      moveDialog.show()
    }
  }

  @NotNull
  private def createMoveClassesOrPackagesToNewDirectoryDialog(@NotNull directory: PsiDirectory,
                                                                         elementsToMove: Array[PsiElement],
                                                                         moveCallback: MoveCallback): DialogWrapper = {
    new MoveClassesOrPackagesToNewDirectoryDialog(directory, elementsToMove, moveCallback) {
      protected override def createCenterPanel(): JComponent = {
        addMoveCompanionChb(super.createCenterPanel(), elementsToMove)
      }
    }
  }

  private def addMoveCompanionChb(@Nullable panel: JComponent, elements: Array[PsiElement]): JComponent = {
    val companionsExist = elements.exists {
      case definition: ScTypeDefinition => definition.baseCompanion.isDefined
      case _ => false
    }

    if (companionsExist) {
      val result = new JPanel(new BorderLayout())
      if (panel != null) result.add(panel, BorderLayout.NORTH)

      val chbMoveCompanion = new JCheckBox(ScalaBundle.message("move.with.companion"))
      val applicationSettings = ScalaApplicationSettings.getInstance()
      chbMoveCompanion.setSelected(applicationSettings.MOVE_COMPANION)
      chbMoveCompanion.addActionListener((_: ActionEvent) => {
        applicationSettings.MOVE_COMPANION = chbMoveCompanion.isSelected
      })
      chbMoveCompanion.setMnemonic('t')
      result.add(chbMoveCompanion, BorderLayout.WEST)
      result
    }
    else panel
  }
}
