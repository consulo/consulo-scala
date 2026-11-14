package org.jetbrains.plugins.scala.codeInspection.cast

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.localize.LocalizeValue
import consulo.language.editor.impl.inspection.ProblemDescriptorImpl
import consulo.language.editor.inspection.{LocalInspectionTool, ProblemHighlightType, ProblemsHolder}
import consulo.project.Project
import consulo.document.util.TextRange
import consulo.language.psi.{PsiElement, PsiElementVisitor}
import com.intellij.java.language.psi.PsiMethod
import org.jetbrains.plugins.scala.codeInspection.{AbstractFixOnTwoPsiElements, PsiElementVisitorSimple, ScalaInspectionBundle, conformsToTypeFromClass}
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScExpression, ScPostfixExpr, ScReferenceExpression, ScUnderscoreSection}
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.synthetic.ScSyntheticFunction
import org.jetbrains.plugins.scala.lang.psi.types.{Context, ScTypeExt, TypePresentationContext}

@ExtensionImpl
class ScalaRedundantConversionInspection extends LocalInspectionTool {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.redundant.conversion.inspection"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.general"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE


  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case element @ ScReferenceExpression.withQualifier(qualifier) & PsiReferenceEx.resolve(target) =>
      process(element, qualifier, target, qualifier.getTextLength, holder)
    case element @ ScPostfixExpr(operand, operator @ PsiReferenceEx.resolve(target)) =>
      process(element, operand, target, operator.getStartOffsetInParent, holder)
    case _ =>
  }

  private def process(element: PsiElement, left: ScExpression, target: PsiElement, offset: Int, holder: ProblemsHolder): Unit = {
    implicit val tpc: TypePresentationContext = TypePresentationContext(element)
    implicit val context: Context = Context(element)

    target match {
      case f: ScSyntheticFunction if f.name.startsWith("to") =>
        for {
          leftType <- left.`type`().toOption
          conversionType = f.retType if leftType.widen.equiv(conversionType)
        } registerProblem(element, left, conversionType.presentableText, offset, holder)
      case f: PsiMethod if f.name == "toString" &&
              f.getParameterList.getParametersCount == 0 &&
              (f.getTypeParameterList == null || f.getTypeParameterList.getTypeParameters.isEmpty) =>
        for {
          leftType <- left.`type`().toOption
          if conformsToTypeFromClass(leftType, "java.lang.String")(using element, context)
        } registerProblem(element, left, "java.lang.String", offset, holder)
      case _ =>
    }
  }

  private def registerProblem(element: PsiElement, left: ScExpression, conversionType: String,
                      offset: Int, holder: ProblemsHolder): Unit = {
    val descriptor = {
      val range = new TextRange(offset, element.getTextLength)

      val message = ScalaInspectionBundle.message("casting.a.to.b.is.redundant", left.getText, conversionType)

      new ProblemDescriptorImpl(element, element, LocalizeValue.of(message), Array(new RemoveConversionQuickFix(element, left)),
        ProblemHighlightType.LIKE_UNUSED_SYMBOL, false, range, null, false)
    }

    holder.registerProblem(descriptor)
  }

  private class RemoveConversionQuickFix(element: PsiElement, expr: ScExpression)
          extends AbstractFixOnTwoPsiElements(ScalaInspectionBundle.message("remove.redundant.conversion"), element, expr) {

    override protected def doApplyFix(elem: PsiElement, scExpr: ScExpression)
                                     (implicit project: Project): Unit = {
      scExpr match {
        case under: ScUnderscoreSection if under.overExpr.contains(elem) =>
          elem.replace(ScalaPsiElementFactory.createIdentifier("identity").getPsi)
        case _ =>
          elem.replace(scExpr)
      }
    }
  }
}


