package org.jetbrains.plugins.scala.worksheet

import consulo.annotation.component.ExtensionImpl
import consulo.virtualFileSystem.fileType.{FileTypeConsumer, FileTypeFactory}

// upstream: <fileType name="Scala Worksheet" language="Scala Worksheet" extensions="sc"/>
@ExtensionImpl
final class WorksheetFileTypeFactory extends FileTypeFactory {
  override def createFileTypes(consumer: FileTypeConsumer): Unit =
    consumer.consume(WorksheetFileType, "sc")
}
