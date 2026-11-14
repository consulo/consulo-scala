package org.jetbrains.bsp.project.test.environment

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import consulo.project.Project
import consulo.ui.ComboBox
import consulo.ui.ex.awt.DialogWrapper
import consulo.ui.ex.awtUnsafe.TargetAWT
import org.jetbrains.bsp.BspBundle

import java.awt.{BorderLayout, Dimension, Toolkit}
import java.net.URI
import javax.swing.{JComponent, JPanel}
import scala.jdk.CollectionConverters.*

object BspSelectTargetDialog {
  def promptForBspTarget(
    project: Project,
    targetIds: Seq[BuildTargetIdentifier],
    selected: Option[BuildTargetIdentifier]
  ): Option[BuildTargetIdentifier] = {
    var result: Option[BuildTargetIdentifier] = None
    val dialog = new BspSelectTargetDialog(project, targetIds, selected)
    if (dialog.showAndGet()) {
      result = dialog.selectedItem
    }
    result
  }

  private[environment] def visibleNames(targetIds: Seq[BuildTargetIdentifier]): Seq[String] = {
    targetIds.map(visibleName)
  }

  private def visibleName(id: BuildTargetIdentifier): String = {
    val targetUri = new URI(id.getUri)
    val fromQuery = getQueryAsMap(targetUri).get("id").flatten
    //Example:
    //targetUri = file:/C:/Users/myUser/myProject/#root/Compile -> fragment = root/Compile
    //targetUri = file:/C:/Users/myUser/myProject/#root/Test -> fragment = root/Test
    val orFromFragment = fromQuery.orElse(Option(targetUri.getFragment))
    orFromFragment.getOrElse(id.getUri)
  }

  private def getQueryAsMap(uri: URI): Map[String, Option[String]] = {
    val query = uri.getQuery
    if (query == null)
      Map.empty
    else
      query.split("&")
        .map(_.split("=", 2))
        .map(item => item(0) -> item.lift(1)).toMap
  }
}

class BspSelectTargetDialog(
  project: Project,
  targetIds: Seq[BuildTargetIdentifier],
  selected: Option[BuildTargetIdentifier]
) extends DialogWrapper(project, true) {
  // upstream: Swing ComboBox
  private val visibleNames = BspSelectTargetDialog.visibleNames(targetIds)
  private val combo: ComboBox[String] = ComboBox.create(visibleNames.asJava)

  locally {
    setTitle(BspBundle.message("bsp.task.choose.target.title"))
    init()
  }

  def selectedItem: Option[BuildTargetIdentifier] =
    Option(combo.getValue).map(visibleNames.indexOf(_)).flatMap(targetIds.lift)

  override def createCenterPanel(): JComponent = null

  override def createNorthPanel(): JComponent = {
    val selectedIndex = selected.map(targetIds.indexOf(_)).getOrElse(-1)
    if (selectedIndex >= 0) combo.setValueByIndex(selectedIndex)
    val comboComponent = TargetAWT.to(combo)
    val width = Toolkit.getDefaultToolkit.getScreenSize.width / 4
    comboComponent.setMinimumSize(new Dimension(width, comboComponent.getPreferredSize.height))
    comboComponent.setPreferredSize(new Dimension(width, comboComponent.getPreferredSize.height))
    val panel = new JPanel(new BorderLayout)
    panel.add(comboComponent)
    panel
  }
}