package org.jetbrains.plugins.scala.lang.refactoring.inline

import consulo.language.editor.TargetElementUtil
import consulo.application.progress.ProgressManager
import consulo.localize.LocalizeValue
import consulo.ui.RadioButton
import consulo.ui.layout.VerticalLayout
import consulo.util.lang.function.ThrowableSupplier
import consulo.codeEditor.Editor
import consulo.project.Project
import consulo.language.psi.PsiReference
import consulo.language.editor.refactoring.BaseRefactoringProcessor
import consulo.language.editor.refactoring.inline.InlineOptionsDialog
import consulo.language.editor.refactoring.inline.InlineOptionsDialog.initOccurrencesNumber
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.extensions.{ObjectExt, OptionExt, ToNullSafe, inReadAction}
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScReferenceExpression
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScNamedElement
import org.jetbrains.plugins.scala.settings.ScalaApplicationSettings

abstract class ScalaInlineDialog(element: ScNamedElement, title: String, helpId: String)
                                (implicit project: Project, editor: Editor) extends InlineOptionsDialog(project, true, element) {
  protected val occurrencesNumber: Int =
    ProgressManager.getInstance.runProcessWithProgressSynchronously[Integer, RuntimeException](
      (() => Integer.valueOf(inReadAction(initOccurrencesNumber(element)))): ThrowableSupplier[Integer, RuntimeException],
      LocalizeValue.of(ScalaBundle.message("inline.occurrences.calculate.progress.title")), true, project).intValue

  protected final val referenceAtCaret: Option[PsiReference] =
    TargetElementUtil
      .findReference(editor, editor.getCaretModel.getOffset)
      .toOption

  // Needs to be lazy as it is referenced in the init block. Otherwise, it will be null if overridden
  protected lazy val reference: Option[PsiReference] =
    referenceAtCaret.filterByType[ScReferenceExpression]

  locally {
    // must be set before `init()` call
    myInvokedOnReference = reference.isDefined && occurrencesNumber != 1

    init()
    setTitle(title)
  }

  protected final def settings: ScalaApplicationSettings = ScalaApplicationSettings.getInstance()

  protected def inlineThisGetter: ScalaApplicationSettings => Boolean
  protected def inlineThisSetter: (ScalaApplicationSettings, Boolean) => Unit
  override def isInlineThis: Boolean = inlineThisGetter(settings)

  protected def inlineKeepGetter: ScalaApplicationSettings => Boolean
  protected def inlineKeepSetter: (ScalaApplicationSettings, Boolean) => Unit
  protected def isKeepTheDeclarationByDefault: Boolean = inlineKeepGetter(settings)

  // upstream: "keep the declaration" option of IntelliJ InlineOptionsDialog (not in Consulo InlineOptionsDialog)
  protected var myKeepTheDeclaration: RadioButton = scala.compiletime.uninitialized

  @Nls protected def getKeepTheDeclarationText: String = null

  def isKeepTheDeclaration: Boolean =
    myKeepTheDeclaration != null && myKeepTheDeclaration.getValueOrError.booleanValue

  override protected def createCenterUIComponent(): VerticalLayout = {
    val layout = super.createCenterUIComponent()
    val keepText = getKeepTheDeclarationText
    if (keepText != null && myElement.isWritable) {
      val keep = myInlineGroup.newButton(LocalizeValue.of(keepText), null)
      myKeepTheDeclaration = keep
      layout.add(keep)
      if (myInvokedOnReference && canInlineThisOnly()) {
        keep.setEnabled(false)
      } else if (myRbInlineAll.getValueOrError.booleanValue && isKeepTheDeclarationByDefault) {
        myRbInlineAll.setValue(false)
        keep.setValue(true)
      }
    }
    layout
  }

  @Nls protected def nameLabelText: String
  @Nls protected def inlineThisText: String

  final override protected def getNameLabelText: LocalizeValue = LocalizeValue.of(nameLabelText)
  final override protected def getInlineThisText: LocalizeValue = LocalizeValue.of(inlineThisText)
  override protected def getBorderTitle: LocalizeValue = LocalizeValue.of(title)

  protected def createProcessor(): BaseRefactoringProcessor

  @Nls protected def getInlineAllUsagesAndRemoveDefinitionText: String
  final override def getInlineAllText: LocalizeValue = LocalizeValue.of(
    if (element.isWritable) getInlineAllUsagesAndRemoveDefinitionText
    else ScalaBundle.message("inline.all.usages.in.project"))

  override val getHelpId: String = helpId

  override def doAction(): Unit = {
    invokeRefactoring(createProcessor())
    updateSettings()
  }

  private def updateSettings(): Unit = {
    if (myRbInlineThisOnly.isEnabled && myRbInlineAll.isEnabled) {
      inlineThisSetter(settings, isInlineThisOnly)
    }
    if (myKeepTheDeclaration.nullSafe.exists(_.isEnabled)) {
      inlineKeepSetter(settings, isKeepTheDeclaration)
    }
  }
}
