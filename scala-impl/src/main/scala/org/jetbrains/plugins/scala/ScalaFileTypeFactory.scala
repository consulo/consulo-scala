package org.jetbrains.plugins.scala

import consulo.annotation.component.ExtensionImpl
import consulo.virtualFileSystem.fileType.{FileTypeConsumer, FileTypeFactory}

/**
 * @author VISTALL
 * @since 2026-10-24
 */
@ExtensionImpl
class ScalaFileTypeFactory extends FileTypeFactory:
  override def createFileTypes(consumer: FileTypeConsumer): Unit =
    consumer.consume(ScalaFileType, "scala;sc")
