package org.jetbrains.plugins.scala.codeInsight.intention.collections

import consulo.annotation.component.ExtensionImpl
import consulo.language.editor.intention.IntentionMetaData

import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.codeInsight.intention.OperationOnCollectionSimplificationBasedIntention
import org.jetbrains.plugins.scala.codeInspection.collections.FilterSetContainsInspection

@ExtensionImpl
@IntentionMetaData(ignoreId = "scala.FilterSetContainsIntention", fileExtensions = Array("scala"), categories = Array("Scala", "Collections"))
final class FilterSetContainsIntention extends OperationOnCollectionSimplificationBasedIntention(
  ScalaBundle.message("family.name.filter.set.contains"),
  FilterSetContainsInspection
)
