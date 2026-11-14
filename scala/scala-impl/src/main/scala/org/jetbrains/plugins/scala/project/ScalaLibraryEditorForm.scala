package org.jetbrains.plugins.scala.project

import consulo.content.library.ui.LibraryEditorComponent
import consulo.disposer.Disposable
import consulo.fileChooser.{FileChooserDescriptor, FileChooserDescriptorFactory, FileChooserTextBoxBuilder}
import consulo.localize.LocalizeValue
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.layout.VerticalLayout
import consulo.ui.model.{FlatDataModel, MutableFlatDataModel}
import consulo.ui.util.LabeledBuilder
import consulo.ui.{ComboBox, Component, Label}
import consulo.virtualFileSystem.util.{VirtualFileUtil => VfsUtilCore}
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.api.project.ScalaLanguageLevel
import org.jetbrains.plugins.scala.project.ScalaLibraryEditorForm._
import org.jetbrains.plugins.scala.util.ui.distribution.DistributionComboBoxUtils

import scala.jdk.CollectionConverters._

class ScalaLibraryEditorForm(
  editorComponent: LibraryEditorComponent[ScalaLibraryProperties],
  uiDisposable: Disposable
) {
  final private val myClasspathEditor = new MyPathEditor(new FileChooserDescriptor(true, false, true, true, false, true))

  private val myLanguageLevels: MutableFlatDataModel[ScalaLanguageLevel] =
    FlatDataModel.of(publishedScalaLanguageLevels.toSeq.asJava)

  private val myLanguageLevel: ComboBox[ScalaLanguageLevel] = {
    val combo = ComboBox.create(myLanguageLevels)
    combo.setTextRenderer(level => LocalizeValue.of(if (level == null) "" else level.getVersion))
    combo
  }

  // upstream: DistributionComboBox with "bundled" and local jar distributions, empty path means "bundled"
  private val myCompilerBridgeBinaryJarDistribution: FileChooserTextBoxBuilder.Controller = {
    val controller = FileChooserTextBoxBuilder.create(editorComponent.getProject)
      .fileChooserDescriptor(FileChooserDescriptorFactory.createSingleLocalFileDescriptor())
      .uiDisposable(uiDisposable)
      .build()
    controller.getComponent.setPlaceholder(LocalizeValue.of(ScalaBundle.message("bundled.distribution.info.name")))
    DistributionComboBoxUtils.installLocalDistributionInfoPointsToExistingJarFileValidator(controller.getComponent)
    controller
  }

  val contentPanel: Component = {
    val panel = VerticalLayout.create()
    panel.add(LabeledBuilder.simple(LocalizeValue.of(ScalaBundle.message("scala.library.editor.form.scala.version")), myLanguageLevel))
    panel.add(LabeledBuilder.filled(LocalizeValue.of(ScalaBundle.message("scala.library.editor.form.compiler.bridge.jar")), myCompilerBridgeBinaryJarDistribution))
    panel.add(Label.create(LocalizeValue.of(ScalaBundle.message("scala.library.editor.form.compiler.classpath"))))
    panel.add(myClasspathEditor.createUIComponent())
    //NOTE: after we split Scala SDK and Scala library, this label is not very informative because library jars
    // are mostly always empty (unless it's an IntelliJ project)
    panel.add(Label.create(LocalizeValue.of(ScalaBundle.message("scala.library.editor.form.standard.library"))))
    panel
  }

  @RequiredUIAccess
  def isValid: Boolean =
    myCompilerBridgeBinaryJarDistribution.getComponent.validate()

  def languageLevel: ScalaLanguageLevel = myLanguageLevel.getValue

  @RequiredUIAccess
  def languageLevel_=(languageLevel: ScalaLanguageLevel): Unit = {
    // in case some new major release candidate version of the scala compiler is used
    // we want to display it's language level anyway (it's not added to the combobox when creating new SDK)
    if (languageLevel != null && !myLanguageLevels.asScala.exists(_ == languageLevel)) {
      myLanguageLevels.add(languageLevel, 0)
    }
    myLanguageLevel.setValue(languageLevel)
  }

  def classpath: Array[String] = myClasspathEditor.getPaths
  def classpath_=(classpath: Array[String]): Unit = myClasspathEditor.setPaths(classpath)

  def compilerBridgeBinaryJar: Option[String] = {
    val path = myCompilerBridgeBinaryJarDistribution.getValue.trim
    if (path.isEmpty) None //means "use bundled"
    else Some(VfsUtilCore.pathToUrl(path))
  }

  @RequiredUIAccess
  def compilerBridgeBinaryJar_=(url: String): Unit = {
    val path = if (url == null) "" else MyPathEditor.pathToVirtualFile(url).getPath
    myCompilerBridgeBinaryJarDistribution.setValue(path)
    // upstream: the caret is moved to the start on every content change, so long paths show their beginning
    myCompilerBridgeBinaryJarDistribution.getComponent.moveCaretTo(0)
  }
}

private object ScalaLibraryEditorForm {
  private val publishedScalaLanguageLevels = ScalaLanguageLevel.publishedVersions.reverse
}
