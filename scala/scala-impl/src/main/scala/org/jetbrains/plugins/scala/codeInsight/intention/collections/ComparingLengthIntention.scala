package org.jetbrains.plugins.scala.codeInsight.intention.collections

import consulo.annotation.component.ExtensionImpl
import consulo.language.editor.intention.IntentionMetaData

import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.codeInsight.intention.OperationOnCollectionSimplificationBasedIntention
import org.jetbrains.plugins.scala.codeInspection.collections.ComparingLengthInspection

@ExtensionImpl
@IntentionMetaData(ignoreId = "scala.ComparingLengthIntention", fileExtensions = Array("scala"), categories = Array("Scala", "Collections"))
final class ComparingLengthIntention extends OperationOnCollectionSimplificationBasedIntention(
  ScalaBundle.message("family.name.comparing.length"),
  ComparingLengthInspection
)
