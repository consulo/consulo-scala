package org.jetbrains.plugins.scala.findUsages.factory.dialog

import com.intellij.java.impl.find.findUsages.JavaFindUsagesDialogDescriptor
import consulo.find.localize.FindLocalize
import consulo.find.{FindUsagesHandler, FindUsagesOptions}
import consulo.find.ui.AbstractFindUsagesDialogDescriptor.{isSelected, isToChange}
import consulo.localize.LocalizeValue
import consulo.project.Project
import consulo.ui.Component
import consulo.ui.ex.StateRestoringCheckBoxWrapper
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.extensions.ObjectExt
import org.jetbrains.plugins.scala.findUsages.factory.ScalaTypeDefinitionFindUsagesOptions
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScClass, ScTypeDefinition}

private[findUsages]
class ScalaTypeDefinitionUsagesDialog(element: ScTypeDefinition, project: Project, findUsagesOptions: FindUsagesOptions,
                                      toShowInNewTab: Boolean, mustOpenInNewTab: Boolean, isSingleFile: Boolean,
                                      handler: FindUsagesHandler)
  extends JavaFindUsagesDialogDescriptor[ScalaTypeDefinitionFindUsagesOptions](element, project, findUsagesOptions, toShowInNewTab,
    mustOpenInNewTab, isSingleFile, handler) {
  private var myCbUsages: StateRestoringCheckBoxWrapper = scala.compiletime.uninitialized

  private var myCbOnlyNewInstances: StateRestoringCheckBoxWrapper = scala.compiletime.uninitialized

  private var myCbMembersUsages: StateRestoringCheckBoxWrapper = scala.compiletime.uninitialized
  private var myCbImplementingTypeDefinitions: StateRestoringCheckBoxWrapper = scala.compiletime.uninitialized
  private var myCbCompanionModule: StateRestoringCheckBoxWrapper = scala.compiletime.uninitialized

  override protected def getPreferredFocusedControl: Component =
    if (myCbUsages == null) null else myCbUsages.getComponent

  override def calcFindUsagesOptions(options: ScalaTypeDefinitionFindUsagesOptions): Unit = {
    super.calcFindUsagesOptions(options)
    if (isToChange(myCbUsages)) {
      options.isUsages = isSelected(myCbUsages)
    }
    if (isToChange(myCbMembersUsages)) {
      options.isMembersUsages = isSelected(myCbMembersUsages)
    }
    if (isToChange(myCbImplementingTypeDefinitions)) {
      options.isImplementingTypeDefinitions = isSelected(myCbImplementingTypeDefinitions)
    }
    if (isToChange(myCbCompanionModule)) {
      options.isSearchCompanionModule = isSelected(myCbCompanionModule)
    }
    if (options.isUsages) {
      if (isToChange(myCbOnlyNewInstances)) {
        options.isOnlyNewInstances = isSelected(myCbOnlyNewInstances)
      }
    }
    options.isSkipImportStatements = false
  }

  override protected def addFindWhatOptions(): Unit = {
    val options = getFindUsagesOptions
    myCbUsages = addFindWhatCheckbox(FindLocalize.findWhatUsagesCheckbox(), options.isUsages, true)

    if (element.is[ScClass]) {
      myCbOnlyNewInstances = addFindWhatCheckbox(LocalizeValue.of(ScalaBundle.message("find.what.new.instances.usages")), options.isOnlyNewInstances, true)

      //this mode should be available only from dialog, so unselect it every time
      myCbOnlyNewInstances.setValue(false)
    }

    myCbMembersUsages = addFindWhatCheckbox(LocalizeValue.of(ScalaBundle.message("find.what.members.usages.checkbox")), options.isMembersUsages, true)
    myCbImplementingTypeDefinitions = addFindWhatCheckbox(LocalizeValue.of(ScalaBundle.message("find.what.implementing.type.definitions.checkbox")),
      options.isImplementingTypeDefinitions, true)
    element.baseCompanion.foreach { _ =>
      myCbCompanionModule = addFindWhatCheckbox(LocalizeValue.of(ScalaBundle.message("find.what.companion.module.checkbox")), options.isSearchCompanionModule, true)
    }
  }

  override protected def update(): Unit = {
    val dependentCbs = Seq(
      myCbToSearchForTextOccurrences,
      myCbCompanionModule,
      myCbOnlyNewInstances
    )

    if (isSelected(myCbUsages))
      dependentCbs.foreach(makeSelectable)
    else
      dependentCbs.foreach(makeUnselectable)
  }

  override protected def isOkActionEnabled: Boolean =
    isSelected(myCbUsages) ||
      isSelected(myCbMembersUsages) ||
      isSelected(myCbImplementingTypeDefinitions)

  private def makeSelectable(cb: StateRestoringCheckBoxWrapper): Unit = {
    if (cb != null) cb.makeSelectable()
  }

  private def makeUnselectable(cb: StateRestoringCheckBoxWrapper): Unit = {
    if (cb != null) cb.makeUnselectable(false)
  }
}
