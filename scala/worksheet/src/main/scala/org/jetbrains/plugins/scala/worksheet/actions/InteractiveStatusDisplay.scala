package org.jetbrains.plugins.scala.worksheet.actions

import consulo.platform.base.icon.PlatformIconGroup
import consulo.ui.ex.awt.{AsyncProcessIcon, JBLabel}
import consulo.ui.image.Image
import org.jetbrains.plugins.scala.worksheet.actions.InteractiveStatusDisplay.{createAnimatedIcon, createIconWrapper}
import org.jetbrains.plugins.scala.worksheet.ui.WorksheetUiUtils

import javax.swing.{JComponent, JPanel}

class InteractiveStatusDisplay {

  private val iconPanel = new JPanel()

  private var currentIcon: Option[AsyncProcessIcon] = None

  private val successIcon = createAnimatedIcon(PlatformIconGroup.generalInspectionsok())
  private val failureIcon = createAnimatedIcon(PlatformIconGroup.generalError())

  def addTo(panel: JPanel): Unit = {
    iconPanel.add(createIconWrapper(Image.empty(18)), 0)
    panel.add(iconPanel)
    WorksheetUiUtils.fixUnboundMaxSize(iconPanel)
  }

  def onStartCompiling(): Unit = {
    setCurrentIcon(successIcon)
    currentIcon.foreach(_.resume())
  }

  def onFailedCompiling(): Unit =
    setCurrentIcon(failureIcon)

  def onSuccessfulCompiling(): Unit =
    setCurrentIcon(successIcon)

  private def setCurrentIcon(icon: AsyncProcessIcon): Unit = {
    if (!currentIcon.contains(icon)) {
      currentIcon = Some(icon)
      iconPanel.removeAll()
      iconPanel.add(icon, 0)
    }
    currentIcon.foreach(_.suspend())
  }
}

private object InteractiveStatusDisplay {

  // upstream: ScalableIconComponent
  private def createIconWrapper(icon: Image): JComponent =
    new JBLabel(icon)

  // upstream: AnimatedIcon.Default.ICONS frames
  private def createAnimatedIcon(passiveIcon: Image): AsyncProcessIcon =
    new AsyncProcessIcon("Compiling...", Array(Image.busy()), passiveIcon)
}
