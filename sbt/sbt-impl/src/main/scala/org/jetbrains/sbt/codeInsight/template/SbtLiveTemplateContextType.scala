package org.jetbrains.sbt
package codeInsight
package template

import consulo.annotation.component.ExtensionImpl
import consulo.language.editor.template.context.FileTypeBasedContextType
import consulo.localize.LocalizeValue
import org.jetbrains.sbt.api.language.SbtFileType

@ExtensionImpl
final class SbtLiveTemplateContextType extends FileTypeBasedContextType(SbtLiveTemplateContextType.Id, LocalizeValue.of(Sbt.Name), SbtFileType)

object SbtLiveTemplateContextType {
  final val Id = "SBT"
}
