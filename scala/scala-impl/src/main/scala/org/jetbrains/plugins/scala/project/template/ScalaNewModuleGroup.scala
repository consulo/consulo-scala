package org.jetbrains.plugins.scala.project.template

import consulo.localize.LocalizeValue
import consulo.module.creation.scratch.{NewModuleContext, NewModuleContextGroup}

/**
 * The "Scala" group of the new project/module dialog (in "Java Platform"), shared by the Scala, sbt and Scala CLI builders.
 */
object ScalaNewModuleGroup {
  // the order of the items: sbt, Scala CLI, Empty
  final val SbtWeight = 0
  final val ScalaCliWeight = 10
  final val EmptyWeight = 100

  //noinspection ScalaExtractStringToBundle
  def apply(context: NewModuleContext): NewModuleContextGroup =
    context
      .addGroup("jvm", LocalizeValue.localizeTODO("Java Platform"))
      .addGroup("jvm.scala", LocalizeValue.localizeTODO("Scala"))
}
