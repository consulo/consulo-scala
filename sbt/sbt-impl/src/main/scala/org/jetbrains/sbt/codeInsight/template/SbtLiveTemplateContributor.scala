package org.jetbrains.sbt
package codeInsight
package template

import consulo.annotation.component.ExtensionImpl
import consulo.language.editor.template.LiveTemplateContributor
import consulo.localize.LocalizeValue

import scala.util.Using

// upstream: liveTemplates/sbt.xml registered via <defaultLiveTemplates/>
@ExtensionImpl
final class SbtLiveTemplateContributor extends LiveTemplateContributor {

  override def groupId(): String = "sbt"

  override def groupName(): LocalizeValue = LocalizeValue.of("sbt")

  override def contribute(factory: LiveTemplateContributor.Factory): Unit = {
    def template(name: String, value: String, descriptionKey: String): Unit =
      Using.resource(factory.newBuilder("sbt" + name.capitalize, name, value, LocalizeValue.of(SbtBundle.message(descriptionKey)))) { builder =>
        builder.withVariable("ARTIFACT", "complete()", "", true)
        builder.withVariable("GROUP", "complete()", "", true)
        builder.withVariable("VERSION", "complete()", "", true)
        builder.withContext(classOf[SbtLiveTemplateContextType], true)
      }

    template("dep", "\"$GROUP$\" % \"$ARTIFACT$\" % \"$VERSION$\"", "template.for.dependency")
    template("deps", "\"$GROUP$\" %% \"$ARTIFACT$\" % \"$VERSION$\"", "template.for.dependency.built.with.sbt")
  }
}
