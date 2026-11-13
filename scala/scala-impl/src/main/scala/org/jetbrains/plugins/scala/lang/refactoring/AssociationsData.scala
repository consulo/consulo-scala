package org.jetbrains.plugins.scala.lang.refactoring

import consulo.document.util.TextRange

import java.awt.datatransfer.DataFlavor

abstract class AssociationsData(val associations: Array[Association],
                                private val companion: AssociationsData.Companion[?]) {
  // upstream: extends TextBlockTransferableData (copy/paste transferable API is not public in Consulo)

  import AssociationsData._

  final def getOffsetCount: Int = 2 * associations.length

  final def getOffsets(offsets: Array[Int], startIndex: Int): Int =
    this (offsets, startIndex) {
      case (array, index) => array(index) = associations(index).range
    }

  final def setOffsets(offsets: Array[Int], startIndex: Int): Int =
    this (offsets, startIndex) {
      case (array, index) => associations(index).range = array(index)
    }

  final def getFlavor: DataFlavor = companion.flavor

  private def apply(offsets: Array[Int], startIndex: Int)
                   (action: (OffsetsArray, Int) => Unit) = {
    val array = new OffsetsArray(offsets, startIndex)

    for {
      index <- associations.indices
    } action(array, index)

    startIndex + getOffsetCount
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[AssociationsData]

  override def equals(other: Any): Boolean = other match {
    case that: AssociationsData if that.canEqual(this) => associations.sameElements(that.associations)
    case _ => false
  }

  override def hashCode: Int = associations.hashCode
}

object AssociationsData {

  abstract class Companion[D <: AssociationsData](representationClass: Class[D],
                                                  humanPresentableName: String) {
    lazy val flavor = new DataFlavor(representationClass, humanPresentableName)
  }

  private class OffsetsArray(private val offsets: (Array[Int], Int)) extends AnyVal {

    def apply(index: Int): TextRange = {
      val (offsets, offset) = startIndex(index)

      TextRange.create(
        offsets(offset),
        offsets(offset + 1)
      )
    }

    def update(index: Int, range: TextRange): Unit = {
      val (offsets, offset) = startIndex(index)

      offsets(offset) = range.getStartOffset
      offsets(offset + 1) = range.getEndOffset
    }

    private def startIndex(index: Int) = {
      val (offsets, startIndex) = this.offsets
      (offsets, startIndex + 2 * index)
    }
  }

}
