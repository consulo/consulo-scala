package org.jetbrains.sbt.language.completion

import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.completion.{CompletionContributor, CompletionInitializationContext, CompletionParameters, CompletionProvider, CompletionResultSet, CompletionSorter, CompletionType}
import com.intellij.codeInsight.completion.impl.RealPrefixMatchingWeigher
import consulo.language.editor.completion.lookup.LookupElementBuilder
import consulo.language.Language
import consulo.application.dumb.DumbAware
import consulo.language.pattern.ElementPattern
import consulo.language.pattern.PlatformPatterns.psiElement
import consulo.language.psi.{PsiDocumentManager, PsiElement}
import consulo.language.util.ProcessingContext
import org.apache.maven.artifact.versioning.ComparableVersion
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.extensions.PsiElementExt
import org.jetbrains.plugins.scala.lang.completion.{CaptureExt, positionFromParameters}
import org.jetbrains.plugins.scala.lang.psi.api.base.literals.ScStringLiteral
import org.jetbrains.plugins.scala.packagesearch.lang.completion.DependencyVersionWeigher
import org.jetbrains.plugins.scala.packagesearch.lang.completion.DependencyVersionWeigher.DependencyVersion
import org.jetbrains.plugins.scala.packagesearch.util.DependencyUtil
import org.jetbrains.sbt.language.completion.SbtScalaVersionCompletionContributor.SbtScalaVersionCompletionProvider

import scala.jdk.CollectionConverters._

private abstract class SbtScalaVersionCompletionContributor extends CompletionContributor {
  protected def pattern: ElementPattern[? <: PsiElement]

  protected def provider: SbtScalaVersionCompletionProvider

  extend(CompletionType.BASIC, pattern, provider)
}

private object SbtScalaVersionCompletionContributor {
  abstract class SbtScalaVersionCompletionProvider extends CompletionProvider[CompletionParameters] {
    protected def getVersionsByLang(lang: Language, onlyStableVersions: Boolean): Seq[ComparableVersion]

    override def addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet): Unit = {
      val place = positionFromParameters(using parameters)
      val versions = getVersionsByLang(place.getLanguage, onlyStableVersions = !parameters.isExtendedCompletion)
      val sorter = CompletionSorter
        .emptySorter()
        .weigh(new RealPrefixMatchingWeigher)
        .weigh(DependencyVersionWeigher)
      val newResult = result
        .withRelevanceSorter(sorter)
        .withPrefixMatcher(textBeforeDummyIdentifier(place.getText))
      val res = versions.map(getVersionLookupElement)
      newResult.addAllElements(res.asJava)
      newResult.stopHere()
    }

    private def textBeforeDummyIdentifier(text: String): String = {
      val dummyIdx = text.indexOf(CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED)
      val str = if (dummyIdx < 0) text else text.substring(0, dummyIdx)
      str.replaceAll("\"", "")
    }

    protected def getVersionLookupElement(version: ComparableVersion): LookupElementBuilder =
      LookupElementBuilder.create(DependencyVersion(version), version.toString)
  }
}

@ExtensionImpl(id = "scalaVersionCompletionContributor", order = "first, after ml_context")
private class ScalaVersionCompletionContributor extends SbtScalaVersionCompletionContributor with DumbAware {
  override def getLanguage: Language = Language.ANY

  override protected def pattern: ElementPattern[? <: PsiElement] =
    (SbtPsiElementPatterns.sbtFilePattern || SbtPsiElementPatterns.scalaFilePattern) &&
      psiElement().inside(SbtPsiElementPatterns.versionPattern)

  override protected def provider: SbtScalaVersionCompletionProvider = new SbtScalaVersionCompletionProvider {
    override protected def getVersionsByLang(lang: Language, onlyStableVersions: Boolean): Seq[ComparableVersion] =
      if (lang.getID == ScalaLanguage.INSTANCE.getID) {
        val scala2 = DependencyUtil.getScala2CompilerVersions(onlyStableVersions)
        val scala3 = DependencyUtil.getScala3CompilerVersions(onlyStableVersions)

        scala2 ++ scala3
      } else Seq.empty

    override protected def getVersionLookupElement(version: ComparableVersion): LookupElementBuilder =
      super.getVersionLookupElement(version)
        .withInsertHandler { (context, _) =>
          context.commitDocument()
          val caretModel = context.getEditor.getCaretModel
          val element = context.getFile.findElementAt(caretModel.getOffset - 1)
          if (element != null) {
            element.getContext match {
              case str: ScStringLiteral =>
                val document = context.getDocument
                document.replaceString(str.startOffset, str.endOffset, s""""$version"""")
                PsiDocumentManager.getInstance(context.getProject).doPostponedOperationsAndUnblockDocument(document)
              case _ =>
            }
          }
        }
  }
}
