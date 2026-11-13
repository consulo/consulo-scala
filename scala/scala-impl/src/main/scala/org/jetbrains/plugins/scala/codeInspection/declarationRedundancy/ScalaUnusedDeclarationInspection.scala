package org.jetbrains.plugins.scala.codeInspection.declarationRedundancy

import consulo.configurable.{ConfigurableBuilder, UnnamedConfigurable}
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager
import consulo.language.editor.inspection.{InspectionToolState, UpdateInspectionOptionFix}
import consulo.localize.LocalizeValue
import consulo.ui.{ComboBox, Label}
import consulo.util.xml.serializer.XmlSerializerUtil
import consulo.project.Project
import consulo.application.util.HtmlChunk
import com.intellij.java.language.psi.PsiAnnotationOwner
import consulo.language.psi.PsiElement
import org.jetbrains.annotations.{Nls, NonNls}
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.codeInspection.declarationRedundancy.cheapRefSearch.Search.Pipeline
import org.jetbrains.plugins.scala.codeInspection.declarationRedundancy.cheapRefSearch.{ElementUsage, Search, SearchMethodsWithProjectBoundCache}
import org.jetbrains.plugins.scala.extensions.{ObjectExt, PsiElementExt}
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil.{inNameContext, isOnlyVisibleInLocalFile}
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.base.ScNamedTupleComponent
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScNamedConstructorArgPattern
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunctionDeclaration
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScTypeParam
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScNamedElement
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScMember, ScTypeDefinition}
import org.jetbrains.plugins.scala.project.ProjectPsiElementExt
import org.jetbrains.plugins.scala.api.project.ScalaLanguageLevel
import org.jetbrains.plugins.scala.project.settings.ScalaCompilerSettings
import org.jetbrains.plugins.scala.settings.ScalaHighlightingMode
import org.jetbrains.plugins.scala.util.IntentionAvailabilityChecker
import org.jetbrains.plugins.scala.util.SAMUtil.PsiClassToSAMExt

import scala.beans.{BeanProperty, BooleanBeanProperty}

final class ScalaUnusedDeclarationInspection extends HighlightingPassInspection with ScalaUnusedDeclarationIncrementalInspection {

  import ScalaUnusedDeclarationInspection._
  import org.jetbrains.plugins.scala.codeInspection.ui.CompilerInspectionOptions._

  override def isEnabledByDefault: Boolean = true

  override def createStateProvider(): InspectionToolState[?] = new ScalaUnusedDeclarationInspectionState

  // the inspection is also invoked by InspectionBasedHighlightingPass, so options are taken from the current profile
  private def stateFor(element: PsiElement): ScalaUnusedDeclarationInspectionState =
    Option(InspectionProjectProfileManager.getInstance(element.getProject).getCurrentProfile
      .getToolState[ScalaUnusedDeclarationInspectionState](getShortName, element))
      .getOrElse(new ScalaUnusedDeclarationInspectionState)

  override def invoke(element: PsiElement, isOnTheFly: Boolean): Seq[ProblemInfo] = {

    val pipeline = getPipeline(element.getProject, stateFor(element).reportPublicDeclarations)

    def isFunctionDeclarationOfUsedSAMableClass: Boolean = element match {
      case functionDeclaration: ScFunctionDeclaration =>
        val containingClass = functionDeclaration.containingClass
        Option(containingClass).exists(c => c.isSAMable && pipeline.runSearchPipeline(c, isOnTheFly).nonEmpty)
      case _ => false
    }

    element match {
      case typeParam: ScTypeParam if !isOnTheFly || typeParam.hasBounds || typeParam.hasImplicitBounds ||
        typeParam.owner.asOptionOf[ScTypeDefinition].exists(!_.getModifierList.isFinal) => Seq.empty
      case inNameContext(holder: PsiAnnotationOwner) if hasUnusedAnnotation(holder) => Seq.empty
      case named: ScNamedElement =>
        val usages = pipeline.runSearchPipeline(named, isOnTheFly)

        if (usages.isEmpty && !isFunctionDeclarationOfUsedSAMableClass) Seq(unusedProblemInfoFor(named, isOnTheFly))
        else Seq.empty
      case _ => Seq.empty
    }
  }

  protected def unusedProblemInfoFor(named: ScNamedElement, isOnTheFly: Boolean): ProblemInfo = {
    val dontReportPublicDeclarationsQuickFix = if (isOnlyVisibleInLocalFile(named)) None
    else Some(
      (
        new UpdateInspectionOptionFix[ScalaUnusedDeclarationInspection, ScalaUnusedDeclarationInspectionState](
          this,
          LocalizeValue.of(ScalaInspectionBundle.message("fix.unused.declaration.report.public.declarations")),
          _.reportPublicDeclarations = false
        )
      )
    )

    val addScalaAnnotationUnusedQuickFix =
      if (named.scalaLanguageLevelOrDefault < ScalaLanguageLevel.Scala_2_13 || named.is[ScTypeParam]) {
        None
      } else {
        Some(new AddScalaAnnotationUnusedQuickFix(named))
      }

    val message = if (isOnTheFly) {
      ScalaUnusedDeclarationInspection.annotationDescription
    } else {
      UnusedDeclarationVerboseProblemInfoMessage(named)
    }

    ProblemInfo(
      named.nameId,
      message,
      DeleteUnusedElementFix.quickfixesFor(named) ++
        dontReportPublicDeclarationsQuickFix ++
        addScalaAnnotationUnusedQuickFix
    )
  }

  override def shouldProcessElement(element: PsiElement): Boolean = {
    val state = stateFor(element)
    import state._

    def isTopLevelDefinitionInWorksheetFile(m: ScMember): Boolean = {
      val isWorksheetFile = m.getContainingFile.asOptionOf[ScalaFile].exists(_.isWorksheetFile)
      val isTopLevelMember = m.isTopLevel
      isWorksheetFile && isTopLevelMember
    }

    val enabledIfScala3 = enableInScala3 || !element.isInScala3File
    val enabledIfCBH = !disableIfCBHIsUsed || !ScalaHighlightingMode.isShowErrorsFromCompilerEnabled(element.getContainingFile)

    enabledIfScala3 && enabledIfCBH && Search.Util.shouldProcessElement(element) &&
      IntentionAvailabilityChecker.checkInspection(this, element) && {
      element match {
        case _: ScNamedTupleComponent | _: ScNamedConstructorArgPattern => false
        case m: ScMember if isTopLevelDefinitionInWorksheetFile(m) => false
        case n: ScNamedElement =>
          if (isOnlyVisibleInLocalFile(n)) {
            n.nameContext match {
              case m: ScMember if m.isLocal =>
                if (reportLocalDeclarations == AlwaysDisabled) {
                  false
                } else if (reportLocalDeclarations == ComplyToCompilerOption) {
                  ScalaCompilerSettings.forElement(n).toSeq.flatMap(_.additionalCompilerOptions).exists { compilerOption =>
                    compilerOption.equals("-Wunused:locals") ||
                      compilerOption.equals("-Wunused:linted") ||
                      compilerOption.equals("-Xlint:unused")
                  }
                } else {
                  true
                }
              case _ => true
            }
          } else {
            reportPublicDeclarations
          }
        case _ => false
      }
    }
  }

  override def getDescriptionAddendum: HtmlChunk = HtmlChunk.empty
}

object ScalaUnusedDeclarationInspection {
  @Nls
  val annotationDescription: String = ScalaInspectionBundle.message("declaration.is.never.used")

  private[declarationRedundancy] def hasUnusedAnnotation(holder: PsiAnnotationOwner): Boolean =
    holder.hasAnnotation("scala.annotation.unused") ||
      // not entirely correct, but if we find @nowarn here in this situation
      // we can assume that it is directed at the unusedness of the symbol
      holder.hasAnnotation("scala.annotation.nowarn")

  private def getPipeline(project: Project, reportPublicDeclarations: Boolean): Pipeline = {

    val canExit = (_: ElementUsage) => true

    val searcher = SearchMethodsWithProjectBoundCache(project)

    val localSearch = searcher.LocalSearchMethods

    val globalSearch = if (reportPublicDeclarations) {
      searcher.GlobalSearchMethods
    } else Seq.empty

    val extensionPointImplementationSearch = searcher.IJExtensionPointImplementationSearch

    new Pipeline(localSearch ++ globalSearch :+ extensionPointImplementationSearch, canExit)
  }
}

final class ScalaUnusedDeclarationInspectionState extends InspectionToolState[ScalaUnusedDeclarationInspectionState] {

  import org.jetbrains.plugins.scala.codeInspection.ui.CompilerInspectionOptions._

  @BooleanBeanProperty
  var reportPublicDeclarations: Boolean = true

  @BeanProperty
  var reportLocalDeclarations: Int = 0

  @BooleanBeanProperty
  var enableInScala3: Boolean = true //TODO

  @BooleanBeanProperty
  var disableIfCBHIsUsed: Boolean = true

  override def createConfigurable(): UnnamedConfigurable =
    ConfigurableBuilder.newBuilder()
      .checkBox(LocalizeValue.of(ScalaInspectionBundle.message("name.unused.declaration.report.public.declarations")), () => reportPublicDeclarations, reportPublicDeclarations = _)
      .checkBox(LocalizeValue.of(ScalaInspectionBundle.message("enable.in.scala.3")), () => enableInScala3, enableInScala3 = _) //TODO
      .checkBox(LocalizeValue.of(ScalaInspectionBundle.message("disable.when.compiler.based.highlighting.is.used")), () => disableIfCBHIsUsed, disableIfCBHIsUsed = _)
      .component(() => Label.create(LocalizeValue.of(ScalaInspectionBundle.message("name.unused.declaration.report.local.declarations"))))
      .valueComponent[Integer, ComboBox[Integer]](
        () => ComboBox.builder[Integer]()
          .add(Integer.valueOf(AlwaysEnabled), LocalizeValue.of(ScalaInspectionBundle.message("inspection.option.enabled")))
          .add(Integer.valueOf(ComplyToCompilerOption), LocalizeValue.of(ScalaInspectionBundle.message("inspection.option.check.compiler.unnamed")))
          .add(Integer.valueOf(AlwaysDisabled), LocalizeValue.of(ScalaInspectionBundle.message("inspection.option.disabled")))
          .build(),
        () => Integer.valueOf(reportLocalDeclarations),
        (value: Integer) => reportLocalDeclarations = value.intValue()
      )
      .buildUnnamed()

  override def getState: ScalaUnusedDeclarationInspectionState = this

  override def loadState(state: ScalaUnusedDeclarationInspectionState): Unit = XmlSerializerUtil.copyBean(state, this)
}
