package org.jetbrains.plugins.scala.conversion.copy

import consulo.ide.impl.idea.codeInsight.editorActions.TextBlockTransferableData
import org.jetbrains.plugins.scala.lang.refactoring.{Association, Associations, AssociationsData}

// upstream: Associations itself is the TextBlockTransferableData (the API lives in consulo.ide.impl, scala-impl does not use it)
final class AssociationsTransferableData private(override val associations: Array[Association])
  extends AssociationsData(associations, AssociationsTransferableData)
    with TextBlockTransferableData {

  def toAssociations: Associations = Associations(associations)

  override def canEqual(other: Any): Boolean = other.isInstanceOf[AssociationsTransferableData]

  override def toString = s"AssociationsTransferableData(${associations.mkString("Array(", ", ", ")")})"
}

object AssociationsTransferableData
  extends AssociationsData.Companion(classOf[AssociationsTransferableData], "ScalaReferenceData") {

  def apply(associations: Array[Association]): AssociationsTransferableData =
    new AssociationsTransferableData(associations)

  def apply(associations: Associations): AssociationsTransferableData =
    new AssociationsTransferableData(associations.associations)
}
