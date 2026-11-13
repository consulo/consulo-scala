package org.jetbrains.plugins.scala

import consulo.component.extension.ExtensionPointName

import scala.jdk.CollectionConverters._

/**
  * Handy base class for declaring extension points.
  */
abstract class ExtensionPointDeclaration[T](private val name: String) {
  private val extensionPointName = ExtensionPointName.create[T](name)

  def implementations: Seq[T] = {
    extensionPointName.getExtensionList.asScala.toSeq
  }
}
