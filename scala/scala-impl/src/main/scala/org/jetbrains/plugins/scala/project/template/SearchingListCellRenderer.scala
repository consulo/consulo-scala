package org.jetbrains.plugins.scala.project.template

import consulo.ui.ex.awt.JBLabel
import consulo.desktop.awt.ui.impl.image.AnimatedIcon
import consulo.ui.ex.awt.CellRendererPanel

import java.awt.{BorderLayout, Component}
import java.util.concurrent.atomic.AtomicBoolean
import javax.accessibility.AccessibleContext
import javax.swing.{DefaultListCellRenderer, JList}

class SearchingListCellRenderer[T](isSearching: AtomicBoolean, textCustomizer: Option[T => String] = None) extends DefaultListCellRenderer(){

  override def getListCellRendererComponent(
    list: JList[? <: AnyRef],
    value: scala.Any,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean
  ): Component = {
    val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
    textCustomizer.foreach(customizer => setText(customizer(value.asInstanceOf[T])))

    val showProgressIcon = index == -1 && isSearching.get()
    if (showProgressIcon) {
      createPanelWithProgressIcon(component)
    } else {
      component
    }
  }

  private def createPanelWithProgressIcon(component: Component): Component = {
    val panel = new CellRendererPanel(new BorderLayout()) {
      override def getAccessibleContext: AccessibleContext = component.getAccessibleContext
    }
    component.setBackground(null)
    panel.add(component, BorderLayout.WEST)
    val progressIcon = new JBLabel(AnimatedIcon.Default.INSTANCE)
    panel.add(progressIcon, BorderLayout.EAST)
    panel
  }

}
