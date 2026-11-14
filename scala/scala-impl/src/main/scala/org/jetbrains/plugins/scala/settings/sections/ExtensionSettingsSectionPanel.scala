package org.jetbrains.plugins.scala.settings.sections

import consulo.fileChooser.{FileChooser, FileChooserDescriptor}
import consulo.localize.LocalizeValue
import consulo.platform.Platform
import consulo.project.Project
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.ex.awt.{JBLabel, JBList, Messages, ScrollPaneFactory, ToolbarDecorator, UIUtil}
import consulo.ui.ex.awtUnsafe.TargetAWT
import consulo.ui.layout.{LabeledLayout, SplitLayoutPosition, TwoComponentSplitLayout, VerticalLayout}
import consulo.ui.{CheckBox, Component, Hyperlink}
import org.jetbrains.annotations.NonNls
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.components.libextensions.LibraryExtensionsManager._
import org.jetbrains.plugins.scala.components.libextensions._
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings

import java.awt.BorderLayout
import java.nio.file.Paths
import javax.swing._
import scala.annotation.nowarn

class ExtensionSettingsSectionPanel(project: Project) extends SettingsSectionPanel(project) {
  private val rootPanel = VerticalLayout.create()
  private val libraryExtensionsManager = LibraryExtensionsManager.getInstance(project)
  @NonNls
  private val CustomMacrosSupportHelpLink = "https://blog.jetbrains.com/scala/2015/10/14/intellij-api-to-build-scala-macros-support/"

  // Exported components
  private val enabledCB: CheckBox = CheckBox.create(LocalizeValue.of(ScalaBundle.message("enable.loading.external.extensions")), true)

  private def scalaProjectSettings = ScalaProjectSettings.getInstance(project)

  override def getRootPanel: Component = rootPanel

  override def isModified: Boolean =
    scalaProjectSettings.isEnableLibraryExtensions != enabledCB.getValue.booleanValue()

  override def apply(): Unit =
    scalaProjectSettings.setEnableLibraryExtensions(enabledCB.getValue)

  @RequiredUIAccess
  override def reset(): Unit =
    enabledCB.setValue(scalaProjectSettings.isEnableLibraryExtensions)


  class LibraryListModel(val extensionsModel: LibraryDetailsModel) extends AbstractListModel[ExtensionJarData] {
    private val extensionsManager: LibraryExtensionsManager = libraryExtensionsManager
    override def getSize: Int = extensionsManager.getAvailableLibraries.length
    override def getElementAt(i: Int): ExtensionJarData = extensionsManager.getAvailableLibraries(i)
  }

  class LibraryDetailsModel(selectedDescriptor: Option[ExtensionJarData]) extends AbstractListModel[ExtensionDescriptor] {
    override def getSize: Int = myExtensions.length
    override def getElementAt(i: Int): ExtensionDescriptor = myExtensions(i)
    private val myExtensions = selectedDescriptor
      .flatMap(
        _.descriptor
          .getCurrentPluginDescriptor
          .map(_.extensions))
      .getOrElse(Nil)
      .filter(_.isAvailable)
  }

  locally {
    // upstream: UI.PanelFactory.panel(enabledCB).withTooltip(...).withTooltipLink(...)
    enabledCB.setToolTipText(LocalizeValue.of(ScalaBundle.message("idea.will.try.to.search.for.extra.support.for.particular.libraries")))
    val helpLink = Hyperlink.create(
      LocalizeValue.of(ScalaBundle.message("how.to.add.custom.macro.support.help.link.title")),
      _ => Platform.current().openInBrowser(CustomMacrosSupportHelpLink)
    )
    rootPanel.add(SettingsSectionPanel.row(enabledCB, helpLink))


    val detailsModel = new LibraryDetailsModel(None)
    val extensionsList = new JBList[ExtensionDescriptor](detailsModel)
    val extensionsPane = new JPanel(new BorderLayout())
    extensionsPane.add(ScrollPaneFactory.createScrollPane(extensionsList))
    extensionsList.setEmptyText(ScalaBundle.message("select.library.from.the.list.above"))

    val extensionsListCellRenderer: java.util.function.Function[ExtensionDescriptor, JBLabel] = {
      case ExtensionDescriptor(_, impl, name, description, _) =>
        val builder = new StringBuilder()
        if (name.nonEmpty) builder.append(name) else builder.append(impl)
        if (description.nonEmpty) builder.append(s" - $description")
        new JBLabel(builder.mkString)
    }
    extensionsList.installCellRenderer(extensionsListCellRenderer): @nowarn("cat=deprecation")

    val libraryListModel = new LibraryListModel(detailsModel)
    val librariesList = new JBList[ExtensionJarData](libraryListModel)
    val toolbarDecorator = ToolbarDecorator.createDecorator(librariesList)

    toolbarDecorator.disableUpDownActions()
    toolbarDecorator.setRemoveAction { _ =>
      val descriptor = librariesList.getSelectedValue
      if (descriptor != null) {
        libraryExtensionsManager.removeExtension(descriptor)
        librariesList.setModel(new LibraryListModel(detailsModel))
        extensionsList.setModel(detailsModel)
      }
    }

    toolbarDecorator.setAddAction { _ =>
      // upstream: synchronous FileChooser.chooseFile
      FileChooser.chooseFile(
        new FileChooserDescriptor(false, false, true, true, false, false),
        project, null).thenAccept { jar =>
      if (jar != null)
        try {
          libraryExtensionsManager.addExtension(Paths.get(jar.getCanonicalPath))
          librariesList.setModel(new LibraryListModel(detailsModel))
        } catch {
          case ex: ExtensionException =>
            //noinspection ReferencePassedToNls
            Messages.showErrorDialog(ex.getMessage, ScalaBundle.message("title.failed.to.load.extension.jar"))
          case ex: Exception =>
            //noinspection ReferencePassedToNls
            Messages.showErrorDialog(ex.toString, ScalaBundle.message("title.failed.to.load.extension.jar"))
        }
      }
    }

    librariesList.setEmptyText(ScalaBundle.message("no.known.extension.libraries"))
    librariesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
    librariesList.addListSelectionListener { event =>
      if (!event.getValueIsAdjusting) {
        val jarData = librariesList.getSelectedValue
        val model   = new LibraryDetailsModel(Option(jarData))
        extensionsList.setModel(model)
      }
    }
    val librariesListCellRenderer: java.util.function.Function[ExtensionJarData, JBLabel] = {
      case ExtensionJarData(LibraryDescriptor(name, _, description, vendor, version, _), file, _) =>
        val builder = new StringBuilder()
        if (vendor.nonEmpty) builder.append(s"($vendor) ")
        builder.append(s"$name $version")
        if (description.nonEmpty) builder.append(s" - $description")
        val label = new JBLabel(builder.mkString)
        label.setToolTipText(file.toAbsolutePath.toString)
        label
    }
    librariesList.installCellRenderer(librariesListCellRenderer): @nowarn("cat=deprecation")

    val librariesPane = new JPanel(new BorderLayout())
    librariesPane.add(toolbarDecorator.createPanel())

    // the lists with their toolbar stay Swing (no unified UI counterpart for ToolbarDecorator)
    val listsPane = TwoComponentSplitLayout.create(SplitLayoutPosition.VERTICAL).withProportion(60)
    listsPane.setFirstComponent(LabeledLayout.create(LocalizeValue.of(ScalaBundle.message("known.extension.libraries")), TargetAWT.wrap(librariesPane)))
    listsPane.setSecondComponent(LabeledLayout.create(LocalizeValue.of(ScalaBundle.message("extensions.in.selected.library")), TargetAWT.wrap(extensionsPane)))

    enabledCB.addValueListener { event =>
      val enabled = event.getValue.booleanValue()
      libraryExtensionsManager.setEnabled(enabled)
      val detailsModel = new LibraryDetailsModel(None)
      val libraryListModel = new LibraryListModel(detailsModel)
      extensionsList.setModel(detailsModel)
      librariesList.setModel(libraryListModel)
      UIUtil.setEnabled(librariesPane, enabled, true)
      UIUtil.setEnabled(extensionsPane, enabled, true)
    }

    rootPanel.add(listsPane)
  }
}
