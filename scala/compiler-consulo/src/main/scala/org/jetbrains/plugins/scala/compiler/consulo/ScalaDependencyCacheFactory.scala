package org.jetbrains.plugins.scala.compiler.consulo

import consulo.annotation.component.ExtensionImpl
import consulo.compiler.{DependencyCache, DependencyCacheFactory}

@ExtensionImpl
final class ScalaDependencyCacheFactory extends DependencyCacheFactory {
  override def create(cacheDir: String): DependencyCache = new ScalaDependencyCache
}
