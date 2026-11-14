package org.jetbrains.plugins.scala.testingSupport

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.application.ApplicationManager
import consulo.logging.Logger
import consulo.codeEditor.Editor
import consulo.module.Module
import consulo.project.Project
import consulo.util.dataholder.Key
import com.intellij.java.language.psi.{JavaDirectoryService, PsiClass, PsiClassOwner, PsiJavaPackage}
import consulo.application.ApplicationPropertiesComponent
import consulo.language.editor.FileModificationService
import consulo.language.editor.localize.CodeInsightLocalize
import com.intellij.java.impl.testIntegration.createTest.TestGenerator
import consulo.language.psi.{PsiElement, PsiFile}
import consulo.language.psi.util.PsiTreeUtil
import consulo.language.util.ModuleUtilCore
import consulo.localize.LocalizeValue
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.ex.awt.{DialogWrapper, Messages, UIUtil}
import consulo.undoRedo.CommandProcessor
import consulo.virtualFileSystem.VirtualFile
import com.intellij.java.impl.refactoring.util.classMembers.MemberInfo
import com.intellij.java.impl.testIntegration.createTest.CreateTestAction
import com.intellij.java.impl.testIntegration.createTest.CreateTestDialog
import com.intellij.java.impl.testIntegration.JavaTestCreator
import com.intellij.java.language.testIntegration.TestFramework
import consulo.language.util.IncorrectOperationException
import org.jetbrains.annotations.TestOnly
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject
import org.jetbrains.plugins.scala.testingSupport.ScalaTestCreator._
import org.jetbrains.plugins.scala.testingSupport.test.TestFrameworkSetupSupport

import java.util as ju
import scala.jdk.CollectionConverters.SeqHasAsJava

/**
 * @see [[org.jetbrains.plugins.scala.testingSupport.ScalaTestGenerator]]
 * @note The class is tested in org.jetbrains.plugins.scala.testingSupport.ScalaTestCreatorInSbtProjectsTest
 */
@ExtensionImpl
class ScalaTestCreator extends JavaTestCreator {
  override def getLanguage: Language = ScalaLanguage.INSTANCE


  override def createTest(project: Project, editor: Editor, file: PsiFile): Unit = {
    try {
      val element = findElement(file, editor.getCaretModel.getOffset)
      if (CreateTestAction.isAvailableForElement(element)) {
        val action = new ScalaCreateTestAction()
        action.invoke(project, editor, element)
      }
    } catch {
      case e: IncorrectOperationException => Log.warn(e)
    }
  }
}

object ScalaTestCreator {

  private val Log = Logger.getInstance("org.jetbrains.plugins.scala.ScalaTestCreator")

  private def findElement(file: PsiFile, offset: Int) = {
    var element = file.findElementAt(offset)
    if (element == null && offset == file.getTextLength) element = file.findElementAt(offset - 1)
    element
  }

  /**
   * upstream: overrides `CreateTestAction.createTestDialog`, Consulo's CreateTestAction creates the dialog in `invoke`,
   * so its flow is repeated here with [[ScalaCreateTestDialog]]
   */
  private class ScalaCreateTestAction extends CreateTestAction {
    @RequiredUIAccess
    override def invoke(project: Project, editor: Editor, element: PsiElement): Unit = {
      if (!FileModificationService.getInstance.preparePsiElementForWrite(element)) return
      val srcModule = ModuleUtilCore.findModuleForPsiElement(element)
      val srcClass = getContainingClass(element)
      if (srcClass == null) return

      val srcDir = element.getContainingFile.getContainingDirectory
      val srcPackage = JavaDirectoryService.getInstance.getPackage(srcDir)

      val propertiesComponent = ApplicationPropertiesComponent.getInstance
      val testFolders = new ju.HashSet[VirtualFile]
      CreateTestAction.checkForTestRoots(srcModule, testFolders)
      if (testFolders.isEmpty && !propertiesComponent.getBoolean(CreateTestInTheSameRoot, false)) {
        if (Messages.showOkCancelDialog(project, "Create test in the same source root?", "No Test Roots Found", UIUtil.getQuestionIcon) != DialogWrapper.OK_EXIT_CODE)
          return
        propertiesComponent.setValue(CreateTestInTheSameRoot, String.valueOf(true))
      }

      val dialog = new ScalaCreateTestDialog(
        project = project,
        title = getText,
        targetClass = srcClass,
        targetPackage = srcPackage,
        targetModule = srcModule
      )
      if (!dialog.showAndGet()) return

      setupFrameworkIfNeeded(project, dialog.getSelectedTestFrameworkDescriptor, srcModule)

      CommandProcessor.getInstance.executeCommand(
        project,
        () => {
          val framework = dialog.getSelectedTestFrameworkDescriptor
          val generator = TestGenerator.forLanguage(framework.getLanguage)
          generator.generateTest(project, dialog)
        },
        CodeInsightLocalize.intentionCreateTest.get,
        this
      )
    }

    // upstream: the "Fix" button of the dialog calls TestFrameworkSetupSupport.setupFramework,
    // Consulo's "Fix" button adds the jar of getLibraryPath to the module, so the setup is offered after OK
    @RequiredUIAccess
    private def setupFrameworkIfNeeded(project: Project, framework: TestFramework, module: Module): Unit =
      framework match {
        case setupSupport: TestFrameworkSetupSupport
          if module != null && !ApplicationManager.getApplication.isUnitTestMode && !framework.isLibraryAttached(module) =>
          val message = LocalizeValue.localizeTODO(s"${framework.getName} library not found in the module. Add it to the build file?")
          if (Messages.showYesNoDialog(project, message.get, getText.get, UIUtil.getQuestionIcon) == Messages.YES)
            setupSupport.setupFramework(module)
        case _ =>
      }

    private def getContainingClass(element: PsiElement): PsiClass =
      PsiTreeUtil.getParentOfType(element, classOf[PsiClass], false) match {
        case null =>
          element.getContainingFile match {
            case owner: PsiClassOwner if owner.getClasses.length == 1 => owner.getClasses()(0)
            case _                                                     => null
          }
        case psiClass => psiClass
      }
  }

  // see com.intellij.java.impl.testIntegration.createTest.CreateTestAction.CREATE_TEST_IN_THE_SAME_ROOT
  private val CreateTestInTheSameRoot = "create.test.in.the.same.root"

  private class ScalaCreateTestDialog(
    project: Project,
    title: LocalizeValue,
    targetClass: PsiClass,
    targetPackage: PsiJavaPackage,
    targetModule: Module,
  ) extends CreateTestDialog(project, title, targetClass, targetPackage, targetModule) {

    def getTargetClass: PsiClass = targetClass

    // upstream: also overrides suggestTestClassName (drops `$` of objects), Consulo's dialog has no such hook

    override def getSelectedTestFrameworkDescriptor: TestFramework = {
      val mock = getMockTestData(project)(_.selectedTestFramework).flatten
      mock.getOrElse(super.getSelectedTestFrameworkDescriptor)
    }

    override def getSelectedMethods: ju.Collection[MemberInfo] = {
      getMockTestData(project)(_.selectedTestedMethodsNames).flatten match {
        case Some(methodNames) =>
          val psiMethods = methodNames.flatMap { name =>
            this.targetClass.findMethodsByName(name, false).toSeq
          }
          psiMethods.map(new MemberInfo(_)).asJava
        case None =>
          super.getSelectedMethods
      }
    }

    override def getClassName: String = {
      val mock = getMockTestData(project)(_.testClassName).flatten
      mock.getOrElse(super.getClassName)
    }

    override def getSuperClassName: String = {
      val mock = getMockTestData(project)(_.superClassName).flatten
      mock.getOrElse(super.getSuperClassName)
    }

    override def shouldGeneratedBefore(): Boolean = {
      val mock = getMockTestData(project)(_.generateBefore)
      mock.getOrElse(super.shouldGeneratedBefore())
    }

    override def shouldGeneratedAfter(): Boolean = {
      val mock = getMockTestData(project)(_.generateAfter)
      mock.getOrElse(super.shouldGeneratedAfter())
    }

    override def showAndGet(): Boolean = {
      if (ApplicationManager.getApplication.isUnitTestMode) {
        doOKAction()
        return true
      }

      super.showAndGet()
    }
  }

  /** upstream: CreateTestDialog.getTargetClass, not exposed by Consulo's dialog */
  def targetClassOf(dialog: CreateTestDialog): PsiClass = dialog match {
    case scalaDialog: ScalaCreateTestDialog => scalaDialog.getTargetClass
    case _                                  => null
  }

  @TestOnly
  case class MockTestDialogData(
    selectedTestFramework: Option[TestFramework] = None,
    testClassName: Option[String] = None,
    selectedTestedMethodsNames: Option[Seq[String]] = None,
    superClassName: Option[String] = None,
    generateBefore: Boolean = false,
    generateAfter: Boolean = false
  )

  @TestOnly
  val MockTestDialogDataKey: Key[MockTestDialogData] =
    Key.create[MockTestDialogData]("scala.test.creator.mock.dialog.data")

  private def getMockTestData[T](project: Project)(f: MockTestDialogData => T): Option[T] =
    if (ApplicationManager.getApplication.isUnitTestMode)
      Option(project.getUserData(MockTestDialogDataKey)).map(f)
    else
      None
}
