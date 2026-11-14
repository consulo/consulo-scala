package org.jetbrains.sbt.language

import consulo.annotation.component.ExtensionImpl
import consulo.virtualFileSystem.fileType.{FileTypeConsumer, FileTypeFactory}
import org.jetbrains.sbt.api.language.SbtFileType

// upstream: <fileType name="sbt" language="sbt" extensions="sbt"/>
@ExtensionImpl
class SbtFileTypeFactory extends FileTypeFactory {
  override def createFileTypes(consumer: FileTypeConsumer): Unit =
    consumer.consume(SbtFileType, "sbt")
}
