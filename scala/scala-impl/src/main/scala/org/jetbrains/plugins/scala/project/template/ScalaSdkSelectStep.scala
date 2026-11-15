package org.jetbrains.plugins.scala.project.template

import com.intellij.java.language.projectRoots.JavaSdkType
import consulo.application.Application
import consulo.content.bundle.SdkTable
import consulo.disposer.Disposable
import consulo.localize.LocalizeValue
import consulo.module.creation.ui.UnifiedProjectOrModuleNameStep
import consulo.module.ui.{BundleBox, BundleBoxBuilder}
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.layout.HorizontalLayout
import consulo.ui.util.FormBuilder
import consulo.ui.{Button, CheckBox, Label}
import consulo.virtualFileSystem.VirtualFileManager
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.project.ScalaLibraryType

/**
 * Name/location, JDK (like consulo-java's JavaSdkSelectStep), the Scala SDK chosen with the Scala SDK selection dialog,
 * the package prefix and whether to add sample code.
 */
final class ScalaSdkSelectStep(context: ScalaNewModuleWizardContext)
  extends UnifiedProjectOrModuleNameStep[ScalaNewModuleWizardContext](context)
    with IndentationSyntaxStepLike
    with PackagePrefixStepLike {

  private var addSampleCodeBox: CheckBox = scala.compiletime.uninitialized

  private var bundleBox: BundleBox = scala.compiletime.uninitialized

  @RequiredUIAccess
  override protected def extend(builder: FormBuilder, uiDisposable: Disposable): Unit = {
    super.extend(builder, uiDisposable)

    val boxBuilder = BundleBoxBuilder.create(uiDisposable)
    boxBuilder.withSdkTypeFilter(_.isInstanceOf[JavaSdkType])
    bundleBox = boxBuilder.build()
    builder.addLabeled(LocalizeValue.localizeTODO("JDK:"), bundleBox.getComponent)

    val comboBox = bundleBox.getComponent
    if (comboBox.getDataModel.getSize > 0) {
      comboBox.setValueByIndex(0)
    }

    val scalaSdkLabel = Label.create(scalaSdkText)
    //noinspection ScalaExtractStringToBundle
    val createButton = Button.create(LocalizeValue.localizeTODO("Create..."), _ => {
      ScalaLibraryType.Description.selectScalaSdk(null).foreach { descriptor =>
        context.scalaSdk = Some(descriptor)
        context.scalaSdkProperties = Some(ScalaLibraryType.Description.resolveProperties(descriptor))
        loadRootsInBackground(descriptor)
        scalaSdkLabel.setText(scalaSdkText)
        setShowIndentationSyntaxCheckBox(context.isScala3)
      }
    })
    val scalaSdkLine = HorizontalLayout.create()
    scalaSdkLine.add(scalaSdkLabel)
    scalaSdkLine.add(createButton)
    builder.addLabeled(LocalizeValue.of(ScalaBundle.message("library.type.scala.sdk") + ":"), scalaSdkLine)
    builder.addLabeled(LocalizeValue.empty(), createIndentationSyntaxCheckBox())
    setShowIndentationSyntaxCheckBox(context.isScala3)
    builder.addLabeled(packagePrefixLabel, createPackagePrefixTextBox(context.packagePrefix))
    addSampleCodeBox = ScalaSampleCode.createCheckBox()
    builder.addBottom(addSampleCodeBox)
  }

  /**
   * The library roots are added in the UI thread when the module is created: their files are found in the VFS before.
   */
  private def loadRootsInBackground(descriptor: ScalaSdkDescriptor): Unit =
    Application.get().executeOnPooledThread((() =>
      ScalaLibraryType.Description.roots(descriptor).foreach { case (url, _) =>
        VirtualFileManager.getInstance.refreshAndFindFileByUrl(url)
      }
    ): Runnable)

  //noinspection ScalaExtractStringToBundle
  private def scalaSdkText: LocalizeValue =
    context.scalaSdk.map(d => LocalizeValue.of(ScalaLibraryType.Description.libraryName(d)))
      .getOrElse(LocalizeValue.localizeTODO("<none>"))

  @RequiredUIAccess
  override def onStepLeave(context: ScalaNewModuleWizardContext): Unit = {
    super.onStepLeave(context)

    context.useIndentationSyntax = useIndentationSyntax
    context.packagePrefix = packagePrefix
    Option(addSampleCodeBox).foreach(box => context.addSampleCode = ScalaSampleCode.isSelected(box))

    Option(bundleBox).flatMap(box => Option(box.getSelectedBundleName)).foreach { name =>
      context.sdk = Option(SdkTable.getInstance.findSdk(name))
    }
  }
}
