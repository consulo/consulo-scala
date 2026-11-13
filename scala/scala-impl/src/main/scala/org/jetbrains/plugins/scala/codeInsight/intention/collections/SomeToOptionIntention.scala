package org.jetbrains.plugins.scala.codeInsight.intention.collections

import consulo.annotation.component.ExtensionImpl
import consulo.language.editor.intention.IntentionMetaData

import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.codeInsight.intention.OperationOnCollectionSimplificationBasedIntention
import org.jetbrains.plugins.scala.codeInspection.collections.SomeToOptionInspection

@ExtensionImpl
@IntentionMetaData(ignoreId = "scala.SomeToOptionIntention", fileExtensions = Array("scala"), categories = Array("Scala", "Collections"))
final class SomeToOptionIntention extends OperationOnCollectionSimplificationBasedIntention(
  ScalaBundle.message("family.name.some.to.option"),
  SomeToOptionInspection
)
