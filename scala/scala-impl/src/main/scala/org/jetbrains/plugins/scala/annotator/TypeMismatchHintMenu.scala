package org.jetbrains.plugins.scala.annotator

import consulo.annotation.component.{ActionImpl, ActionRef}
import consulo.localize.LocalizeValue
import consulo.project.Project
import consulo.ui.ex.action.{AnAction, AnActionEvent, DefaultActionGroup}
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.settings.ShowSettingsUtilImplExt
import org.jetbrains.plugins.scala.settings.sections.EditorSettingsSectionConfigurable

// upstream: an anonymous ActionGroup (MenuProvider(actionGroup)); Consulo inlay renderers only support group ids
@ActionImpl(id = "Scala.ConfigureTypeMismatchHints")
final class ConfigureTypeMismatchHintsAction extends AnAction(LocalizeValue.of(ScalaBundle.message("type.mismatch.hint.context.menu.configure"))) {
  override def actionPerformed(e: AnActionEvent): Unit =
    ShowSettingsUtilImplExt.showSettingsDialog(
      e.getData(Project.KEY),
      classOf[EditorSettingsSectionConfigurable],
      ScalaBundle.message("scala.project.settings.form.show.type.mismatch.hints")
    )
}

@ActionImpl(
  id = "Scala.TypeMismatchHintMenu",
  children = Array(new ActionRef(`type` = classOf[ConfigureTypeMismatchHintsAction]))
)
final class TypeMismatchHintMenuGroup extends DefaultActionGroup
