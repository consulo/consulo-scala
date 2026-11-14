package org.jetbrains.plugins.scala.structureView.sorter

import consulo.platform.base.icon.PlatformIconGroup
import consulo.fileEditor.structureView.tree.{ActionPresentation, ActionPresentationData, Sorter}
import org.jetbrains.annotations.NonNls
import org.jetbrains.plugins.scala.structureView.element.Element

import java.util.Comparator

object ScalaByPositionSorter extends Sorter() {
  @NonNls val ID = "ACTUAL_ORDER_SORTER"

  override val getName: String = ID

  override val isVisible: Boolean = false

  override def getPresentation: ActionPresentation =
    new ActionPresentationData("Sort.actually", "Sort By Position", PlatformIconGroup.objectbrowserSorted())

  override def getComparator: Comparator[?] = (o1: AnyRef, o2: AnyRef) => (o1, o2) match {
    case (e1: Element, e2: Element) if !e1.inherited && !e2.inherited =>
      e1.element.getTextOffset - e2.element.getTextOffset
    case _ => 0
  }
}
