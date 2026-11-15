package org.jetbrains.plugins.scala.project.template

import consulo.localize.LocalizeValue
import consulo.module.creation.scratch.{NewModuleContext, NewModuleContextGroup}
import org.jetbrains.plugins.scala.icons.Icons

/**
 * The "Scala" group of the new project/module dialog (in "Java Platform"), shared by the Scala, sbt and Scala CLI builders.
 */
object ScalaNewModuleGroup {
  //noinspection ScalaExtractStringToBundle
  def apply(context: NewModuleContext): NewModuleContextGroup =
    context
      .addGroup("jvm", LocalizeValue.localizeTODO("Java Platform"))
      .addGroup("jvm.scala", LocalizeValue.localizeTODO("Scala"), Icons.SCALA_SDK)
}
