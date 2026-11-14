package org.jetbrains.plugins.scala.codeInsight.intention.lists

import consulo.document.util.TextRange
import consulo.language.codeStyle.{CodeStyle, CommonCodeStyleSettings}
import consulo.language.psi.PsiElement
import consulo.language.psi.util.PsiTreeUtil
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.extensions.Parent
import org.jetbrains.plugins.scala.lang.formatting.settings.ScalaCodeStyleSettings
import org.jetbrains.plugins.scala.lang.psi.api.base.ScConstructorInvocation
import org.jetbrains.plugins.scala.lang.psi.api.base.types.{ScTupleTypeElement, ScTypeArgs}
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScArgumentExprList, ScBlock, ScMethodCall, ScTuple}
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.{ScParameterClause, ScTypeParamClause}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.templates.ScExtendsBlock

import scala.collection.mutable

// upstream: registered as "listSplitJoinContext" extensions for the Scala language
object ScalaListSplitJoinContexts {
  val All: Seq[ListSplitJoinContext] = Seq(
    new ScalaSplitJoinArgumentsContext,
    new ScalaSplitJoinParametersContext,
    new ScalaSplitJoinTupleTypesContext,
    new ScalaSplitJoinTuplesContext,
    new ScalaSplitJoinTypeArgumentsContext,
    new ScalaSplitJoinTypeParametersContext
  )

  def find(element: PsiElement, joinOrSplit: JoinOrSplit): Option[(ListSplitJoinContext, ListWithElements)] =
    All.iterator.flatMap { context =>
      Option(context.extractData(element)).filter { data =>
        joinOrSplit match {
          case JoinOrSplit.Split => context.isSplitAvailable(data)
          case JoinOrSplit.Join => context.isJoinAvailable(data)
        }
      }.map(context -> _)
    }.nextOption()
}

sealed abstract class AbstractScalaSplitJoinContext extends CommaListSplitJoinContext {
  protected def kind: String

  protected def getCommonSettings(element: PsiElement): CommonCodeStyleSettings =
    CodeStyle.getLanguageSettings(element.getContainingFile)

  protected def getScalaSettings(element: PsiElement): ScalaCodeStyleSettings =
    ScalaCodeStyleSettings.getInstance(element.getProject)

  override def needHeadBreak(data: ListWithElements, firstElement: PsiElement, mode: JoinOrSplit): Boolean =
    mode == JoinOrSplit.Split

  override def needTailBreak(data: ListWithElements, lastElement: PsiElement, mode: JoinOrSplit): Boolean =
    mode == JoinOrSplit.Split

  override final def getJoinText(data: ListWithElements): String =
    ScalaBundle.message("intention.family.put.on.one.line", kind)

  override final def getSplitText(data: ListWithElements): String =
    ScalaBundle.message("intention.family.put.on.separate.lines", kind)

  protected def getParentOfTypeWithinBlock[T <: PsiElement](element: PsiElement, clazz: Class[T]): T =
    PsiTreeUtil.getParentOfType(element, clazz, false, classOf[ScBlock], classOf[ScExtendsBlock])
}

final class ScalaSplitJoinArgumentsContext extends AbstractScalaSplitJoinContext {
  override val kind: String = "arguments"

  override def extractData(element: PsiElement): ListWithElements = {
    val list = getParentOfTypeWithinBlock(element, classOf[ScArgumentExprList])
    list match {
      case Parent(_: ScMethodCall | _: ScConstructorInvocation) =>
        new ListWithElements(list, list.exprs)
      case _ => null
    }
  }

  override def needHeadBreak(data: ListWithElements, firstElement: PsiElement, mode: JoinOrSplit): Boolean =
    mode == JoinOrSplit.Split &&
      getScalaSettings(firstElement).CALL_PARAMETERS_NEW_LINE_AFTER_LPAREN != ScalaCodeStyleSettings.NO_NEW_LINE

  override def needTailBreak(data: ListWithElements, lastElement: PsiElement, mode: JoinOrSplit): Boolean =
    mode == JoinOrSplit.Split && getCommonSettings(lastElement).CALL_PARAMETERS_RPAREN_ON_NEXT_LINE
}

final class ScalaSplitJoinParametersContext extends AbstractScalaSplitJoinContext {
  override val kind: String = "parameters"

  override def extractData(element: PsiElement): ListWithElements = {
    val clause = getParentOfTypeWithinBlock(element, classOf[ScParameterClause])
    if (clause != null)
      new ListWithElements(clause, clause.parameters)
    else
      null
  }

  override def needHeadBreak(data: ListWithElements, firstElement: PsiElement, mode: JoinOrSplit): Boolean =
    mode == JoinOrSplit.Split && getCommonSettings(firstElement).METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE

  override def needTailBreak(data: ListWithElements, lastElement: PsiElement, mode: JoinOrSplit): Boolean =
    mode == JoinOrSplit.Split && getCommonSettings(lastElement).METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE
}

final class ScalaSplitJoinTupleTypesContext extends AbstractScalaSplitJoinContext {
  override val kind: String = "tuple type elements"

  override def extractData(element: PsiElement): ListWithElements = {
    val tupleType = getParentOfTypeWithinBlock(element, classOf[ScTupleTypeElement])
    if (tupleType != null)
      new ListWithElements(tupleType, tupleType.components)
    else
      null
  }

  /* Workaround to properly handle line breaks after `(` when joining lines.
   * Since tuple type components are not direct children of ScTupleTypeElement
   * and firstElement.prevSibling is null.
   */
  override protected def addHeadReplacementsForJoining(data: ListWithElements,
                                                       replacements: mutable.Buffer[(TextRange, String)],
                                                       firstElement: PsiElement): Unit =
    data.list match {
      case tupleType: ScTupleTypeElement =>
        super.addHeadReplacementsForJoining(data, replacements, tupleType.typeList)
      case _ =>
    }

  /* Workaround to properly handle line breaks before `)` when joining lines.
   * Since tuple type components are not direct children of ScTupleTypeElement
   * and lastElement.nextSibling is null.
   */
  override protected def addTailReplacementsForJoining(data: ListWithElements,
                                                       replacements: mutable.Buffer[(TextRange, String)],
                                                       lastElement: PsiElement): Unit =
    data.list match {
      case tupleType: ScTupleTypeElement =>
        super.addTailReplacementsForJoining(data, replacements, tupleType.typeList)
      case _ =>
    }
}

final class ScalaSplitJoinTuplesContext extends AbstractScalaSplitJoinContext {
  override val kind: String = "tuple elements"

  override def extractData(element: PsiElement): ListWithElements = {
    val tuple = getParentOfTypeWithinBlock(element, classOf[ScTuple])
    if (tuple != null)
      new ListWithElements(tuple, tuple.exprs)
    else
      null
  }
}

final class ScalaSplitJoinTypeArgumentsContext extends AbstractScalaSplitJoinContext {
  override val kind: String = "type arguments"

  override def extractData(element: PsiElement): ListWithElements = {
    val list = getParentOfTypeWithinBlock(element, classOf[ScTypeArgs])
    if (list != null)
      new ListWithElements(list, list.typeArguments)
    else
      null
  }
}

final class ScalaSplitJoinTypeParametersContext extends AbstractScalaSplitJoinContext {
  override val kind: String = "type parameters"

  override def extractData(element: PsiElement): ListWithElements = {
    val clause = getParentOfTypeWithinBlock(element, classOf[ScTypeParamClause])
    if (clause != null)
      new ListWithElements(clause, clause.typeParameters)
    else
      null
  }
}
