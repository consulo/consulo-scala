package org.jetbrains.plugins.scala.bazel

import consulo.annotation.access.RequiredReadAction
import consulo.annotation.component.ExtensionImpl
import consulo.language.psi.PsiElement
import org.jetbrains.annotations.Nullable
import org.jetbrains.bazel.ui.gutters.BazelRunConfigurationProducer
import org.jetbrains.bazel.ui.gutters.BazelRunConfigurationProducer.GutterAction
import org.jetbrains.bsp.protocol.BuildTargetInfo
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.incremental.Highlighting.ElementHighlightingExt
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings
import org.jetbrains.plugins.scala.util.ScalaMainMethodUtil

import scala.jdk.CollectionConverters.SeqHasAsJava

/**
 *  - Runs Scala main methods through Bazel, like BazelJavaRunConfigurationProducer does for Java
 *  - Adds logic for running entire test classes and individual tests via Bazel (see [[BazelScalaTestRunLineMarkerLogic]])
 */
@ExtensionImpl(order = "before BazelJavaRunConfigurationProducer")
final class BazelScalaRunConfigurationProducer extends BazelRunConfigurationProducer {

  @Nullable
  @RequiredReadAction
  override def getGutterAction(psiElement: PsiElement, target: BuildTargetInfo): GutterAction = {
    val file = psiElement.getContainingFile
    if (file == null) return null
    if (!file.getLanguage.isKindOf(ScalaLanguage.INSTANCE)) return null

    val project = file.getProject
    if (!ScalaProjectSettings.getInstance(project).isDisableInspections) {
      if (!psiElement.isVisible(project, file)) return null
    }

    if (ScalaMainMethodUtil.hasMain(psiElement)) {
      // main() methods don't need a test filter or additional arguments
      return new GutterAction()
    }

    if (!BazelScalaTestRunLineMarkerLogic.shouldAddMarker(psiElement)) null
    else {
      val testFilter = BazelScalaTestRunLineMarkerLogic.getSingleTestFilter(psiElement)
      if (testFilter == null) return null

      val arguments = BazelScalaTestRunLineMarkerLogic.getExtraProgramArguments(psiElement).asJava
      new GutterAction(testFilter, arguments, java.util.Collections.emptyMap(), null)
    }
  }
}
