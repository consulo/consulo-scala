package org.jetbrains.plugins.scala.lang.completion

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.completion.{CompletionParameters, CompletionProvider, CompletionResultSet, CompletionType}
import consulo.ui.ex.action.{ActionManager, IdeActions}
import consulo.ui.ex.keymap.util.KeymapUtil.getFirstKeyboardShortcutText
import consulo.language.pattern.PlatformPatterns.psiElement
import consulo.language.pattern.{PatternCondition, PsiElementPattern, StandardPatterns}
import consulo.language.psi.PsiElement
import consulo.language.util.ProcessingContext
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.lang.completion.lookups.ScalaLookupItem
import org.jetbrains.plugins.scala.lang.completion.filters.toplevel.IsTopLevelElementInProductionScalaFileFilter
import org.jetbrains.plugins.scala.lang.psi.api.base.ScInterpolatedStringLiteral
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScExpression, ScReferenceExpression, ScSugarCallExpr}

@ExtensionImpl(order = "before scalaClassNameCompletionContributor")
final class ScalaGlobalMembersCompletionContributor extends ScalaCompletionContributor {
  override def getLanguage: Language = ScalaLanguage.INSTANCE


  import ScalaGlobalMembersCompletionContributor._
  import global._

  private val InsideScalaReference_NotTopLevel: PsiElementPattern.Capture[PsiElement] =
    psiElement.withParent(
      StandardPatterns.instanceOf(classOf[ScReferenceExpression])
        .without(TopLevelScalaReferenceInProductionScalaFile)
    )

  extend(
    CompletionType.BASIC,
    InsideScalaReference_NotTopLevel,
    new CompletionProvider {

      override def addCompletions(parameters: CompletionParameters,
                                  context: ProcessingContext,
                                  resultSet: CompletionResultSet): Unit = {
        val reference = positionFromParameters(using parameters)
          .getContext
          .asInstanceOf[ScReferenceExpression]

        val invocationCount = parameters.getInvocationCount

        val finders = reference match {
          case Qualifier(place) =>
            place
              .getTypeWithoutImplicits()
              .toSeq
              .flatMap { originalType =>
                ByTypeGlobalMembersFinder(
                  originalType,
                  place,
                  invocationCount,
                )
              }
          case place =>
            ByPlaceGlobalMembersFinder(
              place,
              resultSet.getPrefixMatcher,
              invocationCount,
            )
        }

        val lookupItems = finders.flatMap(_.lookupItems)

        if (regardlessAccessibility(invocationCount) && !lookupItems.forall {
          case item: ScalaLookupItem => item.shouldImport
          case _ => false
        }) {
          addLookupAdvertisement(resultSet)
        }

        resultSet.addAllElements(lookupItems)
      }
    }
  )
}

object ScalaGlobalMembersCompletionContributor {

  private object TopLevelScalaReferenceInProductionScalaFile extends PatternCondition[ScReferenceExpression]("top-level-scala-reference") {
    override def accepts(ref: ScReferenceExpression, context: ProcessingContext): Boolean =
      IsTopLevelElementInProductionScalaFileFilter.check(ref)
  }

  private def addLookupAdvertisement(resultSet: CompletionResultSet): Unit =
    Option(ActionManager.getInstance.getAction(IdeActions.ACTION_SHOW_INTENTION_ACTIONS))
      .map(getFirstKeyboardShortcutText)
      .map(ScalaBundle.message("to.import.method.statically.press.hotkey", _))
      .foreach(resultSet.addLookupAdvertisement)

  private object Qualifier {

    def unapply(reference: ScReferenceExpression): Option[ScExpression] =
      reference.qualifier.orElse {
        desugaredQualifier(reference)
      }

    private def stringContextQualifier(literal: ScInterpolatedStringLiteral) =
      literal.desugaredExpression.flatMap {
        case (reference, _) => reference.qualifier
      }

    private def desugaredQualifier(reference: ScReferenceExpression) =
      reference.getContext match {
        case ScSugarCallExpr(baseExpression, `reference`, _) => Option(baseExpression)
        case literal: ScInterpolatedStringLiteral => stringContextQualifier(literal)
        case _ => None
      }
  }
}