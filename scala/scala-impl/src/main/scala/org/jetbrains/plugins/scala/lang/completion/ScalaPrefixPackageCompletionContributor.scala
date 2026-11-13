package org.jetbrains.plugins.scala.lang.completion

import consulo.language.editor.CodeInsightSettings
import consulo.language.editor.completion.{CompletionParameters, CompletionProvider, CompletionResultSet, CompletionType}
import consulo.project.Project
import com.intellij.java.language.psi.JavaPsiFacade
import consulo.language.psi.{PsiElement, PsiPackage}
import consulo.language.util.ProcessingContext
import org.jetbrains.plugins.scala.extensions.{PsiNamedElementExt, ResolvesTo, inReadAction}
import org.jetbrains.plugins.scala.lang.completion.ScalaCompletionUtil._
import org.jetbrains.plugins.scala.lang.formatting.settings.ScalaCodeStyleSettings
import org.jetbrains.plugins.scala.lang.psi.api.base.ScReference
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createExpressionWithContextFromText
import org.jetbrains.plugins.scala.lang.resolve.ScalaResolveResult

final class ScalaPrefixPackageCompletionContributor extends ScalaCompletionContributor {

  import ScalaPrefixPackageCompletionContributor._

  extend(
    CompletionType.BASIC,
    identifierWithParentPattern(classOf[ScReference]),
    new CompletionProvider[CompletionParameters] {

      override def addCompletions(parameters: CompletionParameters,
                                  context: ProcessingContext,
                                  result: CompletionResultSet): Unit = {
        val prefixMatcher = result.getPrefixMatcher
        if (parameters.getInvocationCount == 0 ||
          prefixMatcher.getPrefix.isEmpty) return

        val dummyPosition = positionFromParameters(using parameters)
        if (!isInImport(dummyPosition) ||
          shouldRunClassNameCompletion(dummyPosition, prefixMatcher)(using parameters)) return

        for {
          resolveResult <- findApplicablePackages(dummyPosition) {
            prefixMatcher.prefixMatches
          }(using dummyPosition.getProject)

          lookupElement = resolveResult.createLookupElement(isInImport = true)
        } result.addElement(lookupElement)
      }
    }
  )
}

object ScalaPrefixPackageCompletionContributor {

  private def findApplicablePackages(dummyPosition: PsiElement)
                                    (namePredicate: String => Boolean)
                                    (implicit project: Project): Seq[ScalaResolveResult] = for {
    fqn <- prefixPackages

    name = fqn.lastIndexOf('.') match {
      case -1 => fqn
      case dotIndex => fqn.substring(dotIndex)
    }
    if namePredicate(name)

    packageElement <- findPackage(fqn, dummyPosition)
  } yield new ScalaResolveResult(
    packageElement,
    prefixCompletion = true
  )

  private def prefixPackages(implicit project: Project): Seq[String] =
    ScalaCodeStyleSettings
      .getInstance(project)
      .getImportsWithPrefix
      .filterNot(_.startsWith(ScalaCodeStyleSettings.EXCLUDE_PREFIX))
      .toSeq
      .map(_.split('.').dropRight(1).mkString("."))
      .filterNot(_.isEmpty)
      .filterNot(fqn => CodeInsightSettings.getInstance.EXCLUDED_PACKAGES.exists(fqn.startsWith))
      .distinct

  private def findPackage(packageFqn: String,
                                dummyPosition: PsiElement)
                               (implicit project: Project): Option[PsiPackage] =
    inReadAction(JavaPsiFacade.getInstance(project).findPackage(packageFqn)) match {
      case null => None
      case psiPackage =>
        createExpressionWithContextFromText(
          psiPackage.name,
          dummyPosition.getContext,
          dummyPosition
        ) match {
          case ResolvesTo(targetPackage: PsiPackage) if targetPackage.getQualifiedName == psiPackage.getQualifiedName => None
          case _ => Some(psiPackage)
        }
    }
}
