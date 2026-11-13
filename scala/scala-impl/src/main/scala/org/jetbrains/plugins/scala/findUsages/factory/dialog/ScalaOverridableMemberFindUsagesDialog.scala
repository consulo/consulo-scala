package org.jetbrains.plugins.scala.findUsages.factory.dialog

import consulo.find.ui.CommonFindUsagesDialogDescriptor
import consulo.find.{FindUsagesHandler, FindUsagesOptions}
import consulo.find.ui.AbstractFindUsagesDialogDescriptor.{isSelected, isToChange}
import com.intellij.java.language.jvm.JvmModifier
import consulo.localize.LocalizeValue
import consulo.project.Project
import consulo.ui.ex.StateRestoringCheckBoxWrapper
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.findUsages.factory.ScalaMemberFindUsagesOptions
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScMember

/**
 * Implementation of this class was originally inspired by [[com.intellij.java.impl.find.findUsages.FindMethodUsagesDialogDescriptor]]
 */
// upstream: @ApiStatus.Internal
class ScalaOverridableMemberFindUsagesDialog(
  member: ScMember,
  project: Project,
  findUsagesOptions: ScalaMemberFindUsagesOptions,
  toShowInNewTab: Boolean,
  mustOpenInNewTab: Boolean,
  isSingleFile: Boolean,
  handler: FindUsagesHandler
) extends CommonFindUsagesDialogDescriptor(
  member,
  project,
  findUsagesOptions,
  toShowInNewTab,
  mustOpenInNewTab,
  isSingleFile,
  handler
) {
  private var myCbSearchForBase: StateRestoringCheckBoxWrapper = scala.compiletime.uninitialized

  override def calcFindUsagesOptions(options: FindUsagesOptions): Unit = {
    super.calcFindUsagesOptions(options)

    options match {
      case mo: ScalaMemberFindUsagesOptions =>
        if (isToChange(myCbSearchForBase)) {
          mo.isSearchForBaseMember = isSelected(myCbSearchForBase)
        }
      case _ =>
    }
  }

  override protected def addFindWhatOptions(): Unit = {
    if (!member.hasModifier(JvmModifier.PRIVATE)) {
      // upstream: checkbox decorated with comment "use.top.level.hierarchy.members.as.find.usage.targets.by.default"
      val title = LocalizeValue.of(ScalaBundle.message("search.for.base.member.usages"))
      myCbSearchForBase = addFindWhatCheckbox(title, findUsagesOptions.isSearchForBaseMember, true)
    }
  }
}
