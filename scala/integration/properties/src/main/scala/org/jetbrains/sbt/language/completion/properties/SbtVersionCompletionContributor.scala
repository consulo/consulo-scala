package org.jetbrains.sbt.language.completion.properties

import consulo.annotation.component.ExtensionImpl
import consulo.language.editor.completion.lookup.LookupElementBuilder
import consulo.language.Language
import com.intellij.lang.properties.PropertiesLanguage
import com.intellij.lang.properties.psi.impl.PropertyValueImpl
import consulo.application.dumb.DumbAware
import consulo.language.pattern.ElementPattern
import consulo.language.pattern.PlatformPatterns.psiElement
import consulo.language.psi.{PsiDocumentManager, PsiElement}
import org.apache.maven.artifact.versioning.ComparableVersion
import org.jetbrains.plugins.scala.lang.completion.CaptureExt
import org.jetbrains.plugins.scala.packagesearch.util.DependencyUtil
import org.jetbrains.sbt.language.completion.SbtScalaVersionCompletionContributor
import org.jetbrains.sbt.language.completion.SbtScalaVersionCompletionContributor.SbtScalaVersionCompletionProvider
import org.jetbrains.sbt.language.completion.properties.SbtVersionCompletionContributor.{SbtGroupId, SbtLaunchArtifactId}

@ExtensionImpl(id = "sbtVersionCompletionContributor", order = "first, after ml_context")
private class SbtVersionCompletionContributor extends SbtScalaVersionCompletionContributor with DumbAware {
  override def getLanguage: Language = PropertiesLanguage.INSTANCE

  override protected def pattern: ElementPattern[? <: PsiElement] =
    SbtPropertiesPsiElementPatterns.propertiesFilePattern && psiElement().inside(SbtPropertiesPsiElementPatterns.versionPropertyPattern)

  override protected def provider: SbtScalaVersionCompletionProvider = new SbtScalaVersionCompletionProvider {
    override protected def getVersionsByLang(lang: Language, onlyStableVersions: Boolean): Seq[ComparableVersion] =
      if (lang.getID == PropertiesLanguage.INSTANCE.getID) {
        DependencyUtil.getArtifactVersions(SbtGroupId, SbtLaunchArtifactId, onlyStableVersions)
      } else Seq.empty

    override protected def getVersionLookupElement(version: ComparableVersion): LookupElementBuilder =
      super.getVersionLookupElement(version)
        .withInsertHandler { (context, _) =>
          context.commitDocument()
          val caretModel = context.getEditor.getCaretModel
          context.getFile.findElementAt(caretModel.getOffset - 1) match {
            case value: PropertyValueImpl =>
              val document = context.getDocument
              document.replaceString(value.getTextRange.getStartOffset, value.getTextRange.getEndOffset, version.toString)
              PsiDocumentManager.getInstance(context.getProject).doPostponedOperationsAndUnblockDocument(document)
            case _ =>
          }
        }
  }
}

object SbtVersionCompletionContributor {
  private[completion] val SbtGroupId = "org.scala-sbt"
  private[completion] val SbtLaunchArtifactId = "sbt"
}
