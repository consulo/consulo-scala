package org.jetbrains.plugins.scala.findUsages.factory.dialog

import consulo.find.ui.CommonFindUsagesDialog
import consulo.find.{FindUsagesHandler, FindUsagesOptions}
import com.intellij.java.language.jvm.JvmModifier
import consulo.project.Project
import com.intellij.openapi.ui.panel.ComponentPanelBuilder
import consulo.ui.ex.awt.StateRestoringCheckBox
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.findUsages.factory.ScalaMemberFindUsagesOptions
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScMember

import java.awt.Component
import javax.swing.{BoxLayout, JPanel}
import scala.annotation.nowarn

/**
 * Implementation of this class was originally inspired by [[com.intellij.find.findUsages.FindMethodUsagesDialog]]
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
) extends CommonFindUsagesDialog(
  member,
  project,
  findUsagesOptions,
  toShowInNewTab,
  mustOpenInNewTab,
  isSingleFile,
  handler
) {
  private var myCbSearchForBase: StateRestoringCheckBox = scala.compiletime.uninitialized

  override def calcFindUsagesOptions(options: FindUsagesOptions): Unit = {
    super.calcFindUsagesOptions(options)

    options match {
      case mo: ScalaMemberFindUsagesOptions =>
        if (myCbSearchForBase != null) {
          mo.isSearchForBaseMember = myCbSearchForBase.isSelected
        }
      case _ =>
    }
  }

  override def createFindWhatPanel(): JPanel = {
    val findWhatPanel = new JPanel
    findWhatPanel.setLayout(new BoxLayout(findWhatPanel, BoxLayout.Y_AXIS))

    if (!member.hasModifier(JvmModifier.PRIVATE)) {
      val title = ScalaBundle.message("search.for.base.member.usages")
      myCbSearchForBase = createCheckbox(title, findUsagesOptions.isSearchForBaseMember, true)

      @nowarn("cat=deprecation")
      val decoratedCheckbox = new ComponentPanelBuilder(myCbSearchForBase)
        .withComment(ScalaBundle.message("use.top.level.hierarchy.members.as.find.usage.targets.by.default"))
        .createPanel

      decoratedCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT)
      findWhatPanel.add(decoratedCheckbox)
    }

    findWhatPanel
  }
}
