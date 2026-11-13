package org.jetbrains.plugins.scala.codeInspection.controlFlow

import consulo.configurable.{ConfigurableBuilder, UnnamedConfigurable}
import consulo.language.editor.inspection.{InspectionToolState, LocalInspectionTool, LocalInspectionToolSession, ProblemsHolder, UpdateInspectionOptionFix}
import consulo.localize.LocalizeValue
import consulo.util.xml.serializer.XmlSerializerUtil
import consulo.language.psi.PsiElementVisitor
import org.jetbrains.annotations.{Nls, NonNls}
import org.jetbrains.plugins.scala.codeInspection.{PsiElementVisitorSimple, ScalaInspectionBundle}
import org.jetbrains.plugins.scala.lang.parser.ScalaElementType
import org.jetbrains.plugins.scala.lang.psi.api.ScalaPsiElement
import org.jetbrains.plugins.scala.lang.psi.api.expr._
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunctionDefinition

import scala.annotation.tailrec
import scala.beans.BooleanBeanProperty

final class NonLocalReturnInspection extends LocalInspectionTool {

  import NonLocalReturnInspection._
  import org.jetbrains.plugins.scala.codeInspection.ui.CompilerInspectionOptions._

  override def createStateProvider(): InspectionToolState[?] = new NonLocalReturnInspectionState

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession, state: Object): PsiElementVisitor =
    buildVisitor(holder, state.asInstanceOf[NonLocalReturnInspectionState].checkCompilerOption)

  private def buildVisitor(holder: ProblemsHolder, checkCompilerOption: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case scReturn: ScReturn if isNonLocal(scReturn) &&
      isInspectionAllowed(scReturn, checkCompilerOption, "-Xlint:nonlocal-return") =>
      if (!checkCompilerOption) {
        val quickFix = new UpdateInspectionOptionFix[NonLocalReturnInspection, NonLocalReturnInspectionState](
          this, LocalizeValue.of(ScalaInspectionBundle.message("fix.nonlocal.return.check.compiler.option")), _.checkCompilerOption = true)
        holder.registerProblem(scReturn, annotationDescription, quickFix)
      } else {
        holder.registerProblem(scReturn, annotationDescription)
      }
    case _ =>
  }
}

object NonLocalReturnInspection {
  @Nls
  val annotationDescription: String = ScalaInspectionBundle.message("nonlocal.return.statement")

  @Nls
  private[controlFlow] val checkboxLabel: String = ScalaInspectionBundle.message("nonlocal.return.check.compiler.option")

  private def isSynchronized(methodCall: ScMethodCall): Boolean = {
    val ref = methodCall.findFirstChildByTypeScala[ScReferenceExpression](ScalaElementType.REFERENCE_EXPRESSION)
    ref.exists(_.refName.contentEquals("synchronized"))
  }

  @tailrec
  private def isNonLocal(elem: ScalaPsiElement): Boolean = elem.getParent match {
    case _: ScFunctionDefinition => false
    case _: ScFunctionExpr       => true
    case m: ScMethodCall         => !isSynchronized(m)
    case _: ScFor                => true
    case parent: ScalaPsiElement => isNonLocal(parent)
    case _                       => false
  }
}

final class NonLocalReturnInspectionState extends InspectionToolState[NonLocalReturnInspectionState] {
  @BooleanBeanProperty var checkCompilerOption: Boolean = true

  override def createConfigurable(): UnnamedConfigurable =
    ConfigurableBuilder.newBuilder()
      .checkBox(LocalizeValue.of(NonLocalReturnInspection.checkboxLabel), () => checkCompilerOption, checkCompilerOption = _)
      .buildUnnamed()

  override def getState: NonLocalReturnInspectionState = this

  override def loadState(state: NonLocalReturnInspectionState): Unit = XmlSerializerUtil.copyBean(state, this)
}
