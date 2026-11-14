package org.jetbrains.plugins.scala.worksheet

import consulo.annotation.component.ExtensionImpl
import consulo.language.psi.stub.{ObjectStubSerializerProvider, StubElementTypeHolder}

import java.util

/**
 * upstream: languageStubDefinition + stubElementRegistryExtension registering the worksheet file stub serializers,
 * see [[org.jetbrains.plugins.scala.lang.psi.stubs.ScalaStubRegistryExtension]]
 */
@ExtensionImpl
final class WorksheetStubRegistryExtension extends StubElementTypeHolder[AnyRef] {
  override def getExternalIdPrefix: String = null

  override def loadSerializers(): util.List[ObjectStubSerializerProvider] = {
    WorksheetParserDefinition.FileNodeType
    WorksheetParserDefinition3.FileNodeType
    util.List.of()
  }
}
