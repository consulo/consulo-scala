package org.jetbrains.plugins.scala.codeInsight.hints

import consulo.language.editor.inlay.HintInfo.MethodInfo
import consulo.language.editor.inlay.{HintInfo, InlayParameterHintsProvider}
import consulo.language.Language
import consulo.codeEditor.Editor
import consulo.language.psi.{PsiElement, PsiFile}
import com.intellij.java.language.psi.PsiMethod
import consulo.util.collection.ContainerUtil
import org.jetbrains.plugins.scala.incremental.Highlighting._
import org.jetbrains.plugins.scala.api.ScalaLanguage.{INSTANCE => ScalaLanguage}
import org.jetbrains.plugins.scala.annotator.hints.Hint.{HintPosition, MenuProvider}
import org.jetbrains.plugins.scala.annotator.hints.{Hint, Text}
import org.jetbrains.plugins.scala.codeInsight.hints.ScalaInlayParameterHintsPass._
import org.jetbrains.plugins.scala.editor.documentationProvider.ScalaDocQuickInfoGenerator
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.psi.api.{InvocationDetails, InvocationDetailsOwner}
import org.jetbrains.plugins.scala.lang.psi.api.InvocationDetails.ValueClause
import org.jetbrains.plugins.scala.lang.psi.api.base.ScLiteral
import org.jetbrains.plugins.scala.lang.psi.api.expr._
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameter
import org.jetbrains.plugins.scala.lang.psi.types.nonvalue.Parameter
import org.jetbrains.plugins.scala.settings.ScalaApplicationSettings.{getInstance => ScalaApplicationSettings}

import scala.annotation.tailrec
import scala.jdk.CollectionConverters.{CollectionHasAsScala, SeqHasAsJava}

trait ScalaInlayParameterHintsPass {
  protected implicit def settings: ScalaHintsSettings

  protected def collectParameterHints(editor: Editor, root: PsiFile): Seq[Hint] = {
    val filter: HintInfoFilter =
      if (ScalaHintsSettings.xRayMode && ScalaApplicationSettings.XRAY_FOR_ALL_PARAMETERS) _ => true
      else hintInfoFilterFor(ScalaLanguage, ScalaInlayParameterHintsProvider)
    root.elements(_.isVisible(editor.getProject, root)).flatMap(getParameterHints(_, filter)).toSeq
  }

  private def getParameterHints(element: PsiElement, filter: HintInfoFilter): Seq[Hint] = {
    val showParameterHints = settings.showParameters

    val showArgumentHints = ScalaHintsSettings.xRayMode && ScalaApplicationSettings.XRAY_SHOW_ARGUMENT_HINTS

    if (!(showParameterHints || showArgumentHints)) return Seq.empty

    val clauses = element match {
      case owner: InvocationDetailsOwner => owner.invocationDetails.toSeq
        .filterNot(invocation => hintInfoFor(invocation).exists(!filter.showHint(_)))
        .flatMap(_.argumentClauses.collect { case clause: ValueClause => clause })
      case _ => Seq.empty
    }

    clauses.flatMap { clause =>
      // InvocationDetails also supplies default arguments, whose PSI belongs to the declaration.
      val matchedParameters = clause.arguments.filter { case (argument, _) => element.isAncestorOf(argument) }
      (if (showParameterHints) parameterHints(matchedParameters) else Seq.empty) ++
        (if (showArgumentHints) argumentHints(matchedParameters) else Seq.empty)
    } ++ (if (showArgumentHints) referenceHints(element) else Seq.empty)
  }

  private def hintInfoFor(invocation: InvocationDetails): Option[HintInfo] =
    invocation.target.map(_.element).collect { case methodInfo(info) => info }
}

object ScalaInlayParameterHintsPass {
  private[hints] trait HintInfoFilter {
    def showHint(info: HintInfo): Boolean
  }

  // upstream: MethodInfoExcludeListFilter with the user exclude list diff from ParameterNameHintsSettings
  // (both are Consulo internals), only the default exclude lists are applied here
  private def hintInfoFilterFor(language: Language, provider: InlayParameterHintsProvider): HintInfoFilter = {
    val dependencyLanguage = provider.getBlackListDependencyLanguage
    val dependencyExcludeList = Option(InlayParameterHintsProvider.forLanguage(dependencyLanguage))
      .map(_.getDefaultBlackList)
      .getOrElse(java.util.Collections.emptySet[String]())

    val matchers = ContainerUtil.union(provider.getDefaultBlackList, dependencyExcludeList).asScala.flatMap(ExcludeListMatcher(_)).toSeq

    {
      case info: MethodInfo =>
        val paramNames = info.getParamNames.asScala.toSeq
        !matchers.exists(_(info.getFullyQualifiedName, paramNames))
      case _ => false
    }
  }

  private object ExcludeListMatcher {

    def apply(pattern: String): Option[(String, Seq[String]) => Boolean] = {
      val trimmed = pattern.trim
      if (trimmed.isEmpty) return None

      val openIndex = trimmed.lastIndexOf('(')
      val closeIndex = trimmed.lastIndexOf(')')
      if (openIndex < 0) {
        stringMatcher(trimmed).map(method => (name, _) => method(name))
      } else if (closeIndex <= openIndex) {
        None
      } else {
        val inner = trimmed.substring(openIndex + 1, closeIndex)
        val paramPatterns = inner.split(",").map(_.trim).toSeq
        for {
          method <- stringMatcher(trimmed.substring(0, openIndex).trim)
          if inner.nonEmpty && !paramPatterns.exists(_.isEmpty)
          params = paramPatterns.flatMap(stringMatcher)
          if params.size == paramPatterns.size
        } yield (name: String, paramNames: Seq[String]) =>
          method(name) && paramNames.size == params.size && params.zip(paramNames).forall { case (m, n) => m(n) }
      }
    }

    private def stringMatcher(pattern: String): Option[String => Boolean] = {
      val asterisks = pattern.count(_ == '*')
      if (pattern.isEmpty || pattern == "*") Some(_ => true)
      else if (asterisks == 0) Some(_ == pattern)
      else if (asterisks == 1 && pattern.startsWith("*")) Some(_.endsWith(pattern.substring(1)))
      else if (asterisks == 1 && pattern.endsWith("*")) Some(_.startsWith(pattern.dropRight(1)))
      else if (asterisks == 2 && pattern.startsWith("*") && pattern.endsWith("*")) Some(_.contains(pattern.substring(1, pattern.length - 1)))
      else None
    }
  }

  private val menu: MenuProvider = MenuProvider(InlayHintsMenus.ParameterNames)

  private def argumentHints(matchedParameters: Seq[(ScExpression, Parameter)]) = matchedParameters.collect {
    case (argument, parameter) if parameter.isByName =>
      if (argument.is[ScBlockExpr])
        Hint(Seq(Text(" () =>")), argument.getFirstChild, position = HintPosition.AfterElement)
      else
        Hint(Seq(Text("() => ")), argument, position = HintPosition.BeforeElement)
  }

  private def referenceHints(e: PsiElement) = e match {
    case ResolvesTo(parameter: ScParameter) if parameter.isCallByNameParameter =>
      Seq(Hint(Seq(Text("()")), e, position = HintPosition.AfterElement))
    case _ =>
      Seq.empty
  }

  private def parameterHints(matchedParameters: Seq[(ScExpression, Parameter)]) = {
    val (varargs, regular) = matchedParameters.partition {
      case (_, parameter) => parameter.isRepeated
    }

    (regular ++ varargs.headOption).filter {
      case (argument, _) if !isNameable(argument) => false
      case (_: ScUnderscoreSection, _) => false
      case (_, parameter) if parameter.name.isEmpty || !(ScalaHintsSettings.xRayMode && ScalaApplicationSettings.XRAY_FOR_ALL_PARAMETERS) && parameter.name.length == 1 => false
      case (argument, _) => (ScalaHintsSettings.xRayMode && ScalaApplicationSettings.XRAY_FOR_ALL_PARAMETERS) || isUnclear(argument)
    }.map {
      case (argument, parameter) =>

        val tooltip = () => parameter.psiParam.flatMap(ScalaDocQuickInfoGenerator.getQuickNavigateInfo(_, argument))
        Hint(
          Seq(Text(parameter.name, tooltip = tooltip, navigatable = parameter.psiParam), Text(s" ${ScalaTokenTypes.tASSIGN} ")),
          argument,
          position = HintPosition.BeforeElement,
          menu     = menu
        )
    }
  }

  private object methodInfo {

    def unapply(method: PsiMethod): Some[MethodInfo] = {
      val classFqn = method.containingClass match {
        case null => ""
        case clazz => s"${clazz.qualifiedName}."
      }

      val names = method.parameters.map(_.name)

      Some(new MethodInfo(classFqn + method.name, names.asJava))
    }
  }

  private def isNameable(argument: ScExpression) =
    argument.getParent match {
      case list: ScArgumentExprList => list.isArgsInParens
      case _ => false
    }

  @tailrec
  private def isUnclear(expression: ScExpression): Boolean = expression match {
    case _: ScLiteral | _: ScThisReference => true
    case ScParenthesisedExpr(inner) => isUnclear(inner)
    case ScSugarCallExpr(base, _, _) => isUnclear(base)
    case _ => false
  }
}
