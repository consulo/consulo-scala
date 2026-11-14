package org.jetbrains.plugins.scala.structureView.sorter

import consulo.fileEditor.structureView.tree.{ActionPresentation, ActionPresentationData, Sorter, SorterUtil}
import consulo.fileEditor.localize.FileEditorLocalize
import consulo.platform.base.icon.PlatformIconGroup
import org.jetbrains.annotations.NonNls
import org.jetbrains.plugins.scala.structureView.element.Test

import java.util.Comparator

object ScalaAlphaSorter extends Sorter() {
  @NonNls val ID = "ALPHA_SORTER_IGNORING_TEST_NODES"

  override val getName: String = ID

  override val isVisible: Boolean = true

  // TODO move to the implementation of testing support
  override def getComparator: Comparator[?] =
    (o1: AnyRef, o2: AnyRef) => (o1, o2) match {
      case (_: Test, _: Test) => 0
      case (_, _: Test) => -1
      case (_: Test, _) => 1
      case _ => SorterUtil.getStringPresentation(o1).get.compareToIgnoreCase(SorterUtil.getStringPresentation(o2).get)
    }

  override def getPresentation: ActionPresentation = {
    new ActionPresentationData(
      FileEditorLocalize.actionSortAlphabetically(),
      FileEditorLocalize.actionSortAlphabetically(),
      PlatformIconGroup.objectbrowserSorted()
    )
  }
}
