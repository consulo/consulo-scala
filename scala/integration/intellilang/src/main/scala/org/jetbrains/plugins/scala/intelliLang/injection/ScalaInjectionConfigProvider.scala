package org.jetbrains.plugins.scala.intelliLang.injection

import consulo.annotation.component.ExtensionImpl
import consulo.language.inject.advanced.InjectionConfigProvider

import java.io.InputStream

@ExtensionImpl
final class ScalaInjectionConfigProvider extends InjectionConfigProvider {
  override def openConfigFileStream(): InputStream =
    getClass.getResourceAsStream("/org/jetbrains/plugins/scala/intelliLang/injection/scalaInjections.xml")
}
