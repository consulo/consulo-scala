package org.jetbrains.plugins.scala

import consulo.component.extension.ExtensionPointName

import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag

/**
  * Handy base class for declaring extension points.
  */
abstract class ExtensionPointDeclaration[T](private val name: String)(implicit classTag: ClassTag[T]) {
  // Consulo extension points are identified by the extension interface, `name` is kept for reference only
  private val extensionPointName = ExtensionPointName.create[T](classTag.runtimeClass.asInstanceOf[Class[T]])

  def implementations: Seq[T] = {
    extensionPointName.getExtensionList.asScala.toSeq
  }
}
