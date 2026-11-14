package org.jetbrains.sbt.project.module

import consulo.disposer.Disposable
import consulo.externalSystem.service.module.extension.ExternalSystemMutableModuleExtensionImpl
import consulo.localize.LocalizeValue
import consulo.module.content.layer.ModuleRootLayer
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.layout.{DockLayout, LabeledLayout, ScrollableLayout, VerticalLayout}
import consulo.ui.model.FlatDataModel
import consulo.ui.util.FormBuilder
import consulo.ui.{Component, Label, ListBox, Table}
import org.jetbrains.sbt.SbtBundle
import org.jetbrains.sbt.api.resolvers.SbtResolver
import org.jetbrains.sbt.project.SbtProjectSystem
import org.jetbrains.sbt.settings.SbtSettings

import scala.jdk.CollectionConverters.*

// upstream: SbtModuleEditorProvider / SbtModuleSettingsEditor (a read-only module editor tab). Consulo has no module editor
// providers, so the same information is shown on the page of the sbt module extension.
class SbtMutableModuleExtension(id: String, moduleRootLayer: ModuleRootLayer)
  extends ExternalSystemMutableModuleExtensionImpl(id, moduleRootLayer, SbtProjectSystem.Id) {

  @RequiredUIAccess
  override def createConfigurationComponent(uiDisposable: Disposable, updateOnCheck: Runnable): Component = {
    val module = getModule
    val sbtVersion = SbtSettings.getInstance(module.getProject).getLinkedProjectSettings(module)
      .flatMap(settings => Option(settings.sbtVersion))
      .getOrElse(SbtBundle.message("sbt.settings.sbtVersionNotDetected"))

    val versionForm = FormBuilder.create()
    versionForm.addLabeled(LocalizeValue.of(SbtBundle.message("sbt.settings.sbtVersionInModule")), Label.create(LocalizeValue.of(sbtVersion)))

    val imports = ListBox.create(SbtModule.Imports(module).asJava)

    val resolvers = Table.create(FlatDataModel.of[SbtResolver](SbtModule.Resolvers(module).toSeq.sortBy(_.name).asJava))
    resolvers.addColumn[String](LocalizeValue.of(SbtBundle.message("sbt.settings.resolvers.name")), _.name)
    resolvers.addColumn[String](LocalizeValue.of(SbtBundle.message("sbt.settings.resolvers.url")), _.root)

    val lists = VerticalLayout.create()
    lists.add(LabeledLayout.create(LocalizeValue.of(SbtBundle.message("sbt.settings.implicitImports")), ScrollableLayout.create(imports)))
    lists.add(LabeledLayout.create(LocalizeValue.of(SbtBundle.message("sbt.settings.resolvers")), ScrollableLayout.create(resolvers)))

    val layout = DockLayout.create()
    layout.top(versionForm.build())
    layout.center(lists)
    layout
  }
}
