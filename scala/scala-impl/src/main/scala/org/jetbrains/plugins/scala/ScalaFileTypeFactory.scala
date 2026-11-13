package org.jetbrains.plugins.scala

import consulo.annotation.component.ExtensionImpl
import consulo.virtualFileSystem.fileType.{FileTypeConsumer, FileTypeFactory}
import org.jetbrains.plugins.scala.api.ScalaFileType

/**
 * upstream: registered in plugin.xml as <fileType name="Scala" language="Scala" extensions="scala;mill"/>
 *
 * @author VISTALL
 * @since 2026-10-24
 */
@ExtensionImpl
class ScalaFileTypeFactory extends FileTypeFactory:
  override def createFileTypes(consumer: FileTypeConsumer): Unit =
    consumer.consume(ScalaFileType.INSTANCE, "scala;mill")
