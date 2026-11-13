package org.jetbrains.plugins.scala.codeInspection.source3

import consulo.language.editor.inspection.{InspectionToolState, LocalInspectionTool, LocalInspectionToolSession, LocalQuickFix, ProblemsHolder}
import consulo.configurable.{ConfigurableBuilder, UnnamedConfigurable}
import consulo.localize.LocalizeValue
import consulo.util.xml.serializer.XmlSerializerUtil
import consulo.project.Project
import consulo.language.psi.{PsiElement, PsiElementVisitor, PsiFile}
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.annotator.quickfix.AddCaseToGeneratorQuickfix
import org.jetbrains.plugins.scala.codeInspection.source3.Source3Inspection._
import org.jetbrains.plugins.scala.codeInspection.{AbstractFixOnPsiElement, PsiElementVisitorSimple, ScalaInspectionBundle}
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.formatting.settings.ScalaCodeStyleSettings
import org.jetbrains.plugins.scala.lang.lexer.{ScalaTokenType, ScalaTokenTypes}
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.{ScConstructorPattern, ScNamingPattern, ScSeqWildcardPattern, ScTypePattern}
import org.jetbrains.plugins.scala.lang.psi.api.base.types.{ScCompoundTypeElement, ScWildcardTypeElement}
import org.jetbrains.plugins.scala.lang.psi.api.expr._
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports.{ScImportExpr, ScImportSelector}
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.plugins.scala.lang.psi.types.api.ParameterizedType
import org.jetbrains.plugins.scala.project.ProjectPsiElementExt
import org.jetbrains.sbt.api.language.SbtLanguage

import scala.beans.BeanProperty

class Source3Inspection extends LocalInspectionTool {

  override def isAvailableForFile(file: PsiFile): Boolean = {
    file.isSource3Enabled && {
      // sbt uses -Xsource:3 for the .scala files under project/
      // but not for build.sbt files. So we exclude all sbt files from this inspection.
      // See also the discussion here: https://youtrack.jetbrains.com/issue/SCL-24570
      !file.getLanguage.is(SbtLanguage.INSTANCE)
    }
  }

  override def createStateProvider(): InspectionToolState[?] = new Source3InspectionState

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession, state: Object): PsiElementVisitor =
    buildVisitor(holder, state.asInstanceOf[Source3InspectionState])

  private def buildVisitor(holder: ProblemsHolder, state: Source3InspectionState): PsiElementVisitor = PsiElementVisitorSimple(holder) { element =>
    import state._

    lazy val scala3ImportsAllowed = !ScalaCodeStyleSettings.getInstance(element.getProject).isForceScala2ImportSyntaxInSource3

    val features = element.features

    element match {
      case ScWildcardTypeElementUnderscore(wildcardTypeElement, underscore) if convertWildcardUnderscore && features.`? as wildcard marker` =>
        holder.registerProblem(
          underscore,
          getDisplayName.get,
          createReplacingQuickFix(wildcardTypeElement, ScalaInspectionBundle.message("replace.with.questionmark")) { e =>
            ScalaPsiElementFactory.createTypeElementFromText(e.getText.replaceFirst("_", "?"), e, null)
          }
        )
      case gen@ScGenerator(pattern, _) if addGeneratorCase && features.`case in pattern bindings` && gen.caseKeyword.isEmpty &&
        generatorType(gen).exists(ty => !pattern.isIrrefutableFor(ty)) =>
        holder.registerProblem(
          pattern,
          getDisplayName.get,
          (new AddCaseToGeneratorQuickfix(gen)),
        )
      case ElementType(ScalaTokenTypes.tUNDER) if scala3ImportsAllowed && convertWildcardImport && isUpgradableImportWildcard(element) =>
        holder.registerProblem(
          element,
          getDisplayName.get,
          createReplacingQuickFix(element, ScalaInspectionBundle.message("replace.with.star")) { underscore =>
            ScalaPsiElementFactory.createImportFromText("import a.*", underscore.getContext, null).lastLeaf
          }
        )
      case ElementType(ScalaTokenTypes.tFUNTYPE) if scala3ImportsAllowed && convertImportAlias && features.`Scala 3 renaming imports` && element.getParent.is[ScImportSelector] =>
        holder.registerProblem(
          element,
          getDisplayName.get,
          createReplacingQuickFix(element, ScalaInspectionBundle.message("replace.with.as")) { arrow =>
            ScalaPsiElementFactory.createImportFromText("import a.{x as y}", arrow.getContext, null)
              .importExprs.head.selectors.head.findFirstChildByType(ScalaTokenType.AsKeyword).get
          }
        )
      case typed@ScTypedExpression.sequenceArg(seqArg) if convertVarArgSplices && features.`Scala 3 vararg splice syntax` &&
        seqArg.getFirstChild.elementType == ScalaTokenTypes.tUNDER =>
        holder.registerProblem(
          seqArg,
          getDisplayName.get,
          createReplacingQuickFix(typed, ScalaInspectionBundle.message("replace.with.star")) { typed =>
            val innerText = typed.expr match {
              case _: ScInfixExpr | _: ScPostfixExpr | _: ScPrefixExpr => s"(${typed.expr.getText})"
              case expr if expr.getText.endsWith("_") => typed.expr.getText + " "
              case _ => typed.expr.getText
            }
            ScalaPsiElementFactory.createExpressionWithContextFromText(s"call($innerText*)", typed.getContext, typed)
              .asInstanceOf[ScMethodCall].argumentExpressions.head
          }
        )
      case named@ScNamingPattern(seqWildcard: ScSeqWildcardPattern) if convertNamedWildcardPattern &&
        features.`Scala 3 vararg splice syntax` && seqWildcard.isWildcard =>
        holder.registerProblem(
          named,
          getDisplayName.get,
          createReplacingQuickFix(named, ScalaInspectionBundle.message("replace.with.name.followed.by.star", named.name)) { named =>
            ScalaPsiElementFactory.createPatternFromTextWithContext(s"Seq(${named.name}*)", named.getContext, named)
              .asInstanceOf[ScConstructorPattern].args.patterns.head
          }
        )
      case withKw@ElementType(ScalaTokenTypes.kWITH) & Parent(compoundType: ScCompoundTypeElement) if convertCompoundTypes &&
        features.`& instead of with` && !compoundType.getParent.is[ScTypePattern] =>
        holder.registerProblem(
          withKw,
          getDisplayName.get,
          createReplacingQuickFix(withKw, ScalaInspectionBundle.message("replace.with.and.char")) { withKw =>
            ScalaPsiElementFactory.createIdentifier("&")(using withKw).getPsi
          }
        )
      case _ =>
    }
  }
}

object Source3Inspection {
  //private val `.getText`: Qualified = invocation("getText").from(ArraySeq(psiElementFqn, psiASTNodeFqn))
  //private val `.contains`: Qualified = invocation("contains")

  object ScWildcardTypeElementUnderscore {
    def unapply(wildcardType: ScWildcardTypeElement): Option[(ScWildcardTypeElement, PsiElement)] =
      wildcardType.findFirstChildByType(ScalaTokenTypes.tUNDER).map(wildcardType -> _)
  }

  private def isUpgradableImportWildcard(element: PsiElement): Boolean = {
    // example: import test.{x => _}
    def isNotShadowingAlias = element.prevSibling.forall(_.elementType == ScalaTokenTypes.tDOT)

    def isInRightElement = element.getParent match {
      case _: ScImportExpr => element.features.`Scala 3 wildcard imports`
      case _: ScImportSelector => element.features.`Scala 3 wildcard imports in selector`
      case _ => false
    }

    isInRightElement && isNotShadowingAlias
  }

  private def createReplacingQuickFix[T <: PsiElement](element: T, @Nls name: String)(transform: T => PsiElement): LocalQuickFix =
    new AbstractFixOnPsiElement(name: String, element) {
      override protected def doApplyFix(element: T)(implicit project: Project): Unit = {
        element.replace(transform(element))
      }
    }

  private def generatorType(gen: ScGenerator): Option[ScType] =
    for {
      desugared <- gen.desugared
      (_, param) <- desugared.analogMethodCall.matchedParameters.headOption
      ty <- Some(param.expectedType).collect { case ParameterizedType(_, Seq(p, _)) => p }
    } yield ty
}

final class Source3InspectionState extends InspectionToolState[Source3InspectionState] {
  @BeanProperty var convertWildcardUnderscore: Boolean = true
  @BeanProperty var addGeneratorCase: Boolean = true
  @BeanProperty var convertWildcardImport: Boolean = true
  @BeanProperty var convertImportAlias: Boolean = true
  @BeanProperty var convertVarArgSplices: Boolean = true
  @BeanProperty var convertNamedWildcardPattern: Boolean = true
  @BeanProperty var convertCompoundTypes: Boolean = true

  override def createConfigurable(): UnnamedConfigurable =
    ConfigurableBuilder.newBuilder()
      .checkBox(LocalizeValue.of(ScalaInspectionBundle.message("suggest.converting.wildcards")), () => convertWildcardUnderscore, convertWildcardUnderscore = _)
      .checkBox(LocalizeValue.of(ScalaInspectionBundle.message("suggest.adding.case.in.for.comprehensions")), () => addGeneratorCase, addGeneratorCase = _)
      .checkBox(LocalizeValue.of(ScalaInspectionBundle.message("suggest.using.star.instead.of.underscore")), () => convertWildcardImport, convertWildcardImport = _)
      .checkBox(LocalizeValue.of(ScalaInspectionBundle.message("suggest.using.as.instead.of.arrow")), () => convertImportAlias, convertImportAlias = _)
      .checkBox(LocalizeValue.of(ScalaInspectionBundle.message("suggest.converting.vararg.splices")), () => convertVarArgSplices, convertVarArgSplices = _)
      .checkBox(LocalizeValue.of(ScalaInspectionBundle.message("suggest.converting.named.wildcard.patterns")), () => convertNamedWildcardPattern, convertNamedWildcardPattern = _)
      .checkBox(LocalizeValue.of(ScalaInspectionBundle.message("suggest.using.and.instead.of.with")), () => convertCompoundTypes, convertCompoundTypes = _)
      .buildUnnamed()

  override def getState: Source3InspectionState = this

  override def loadState(state: Source3InspectionState): Unit = XmlSerializerUtil.copyBean(state, this)
}
