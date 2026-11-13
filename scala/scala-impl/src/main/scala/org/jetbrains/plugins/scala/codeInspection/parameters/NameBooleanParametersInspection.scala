package org.jetbrains.plugins.scala.codeInspection.parameters

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.configurable.{ConfigurableBuilder, UnnamedConfigurable}
import consulo.language.editor.inspection.InspectionToolState
import consulo.localize.LocalizeValue
import consulo.util.xml.serializer.XmlSerializerUtil
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle

import scala.beans.BooleanBeanProperty

@ExtensionImpl
class NameBooleanParametersInspection extends NameBooleanParametersInspectionBase {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.name.boolean.parameters"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.general"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def createStateProvider(): InspectionToolState[?] = new NameBooleanParametersInspectionState

  override protected def isIgnoreSingleParameter(state: Object): Boolean =
    state.asInstanceOf[NameBooleanParametersInspectionState].ignoreSingleParameter
}

final class NameBooleanParametersInspectionState extends InspectionToolState[NameBooleanParametersInspectionState] {
  @BooleanBeanProperty var ignoreSingleParameter: Boolean = true

  override def createConfigurable(): UnnamedConfigurable =
    ConfigurableBuilder.newBuilder()
      .checkBox(LocalizeValue.of(ScalaInspectionBundle.message("name.boolean.ignore.single.parameter.methods")), () => ignoreSingleParameter, ignoreSingleParameter = _)
      .buildUnnamed()

  override def getState: NameBooleanParametersInspectionState = this

  override def loadState(state: NameBooleanParametersInspectionState): Unit = XmlSerializerUtil.copyBean(state, this)
}
