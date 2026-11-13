package org.jetbrains.plugins.scala.annotator.gutter

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.localize.LocalizeValue
import consulo.ui.image.Image
import consulo.language.editor.DaemonCodeAnalyzerSettings
import consulo.language.editor.gutter.{GutterIconDescriptor, LineMarkerInfo, LineMarkerProviderDescriptor, MergeableLineMarkerInfo, NavigateAction}
import com.intellij.java.impl.codeInsight.daemon.impl.GutterTooltipHelper
import consulo.application.AllIcons
import consulo.application.AllIcons.Gutter
import consulo.language.psi.PsiNavigationSupport
import consulo.ui.ex.action.IdeActions
import consulo.application.ApplicationManager
import consulo.codeEditor.CodeInsightColors
import consulo.colorScheme.EditorColorsManager
import consulo.codeEditor.markup.{GutterIconRenderer, SeparatorPlacement}
import consulo.application.progress.ProgressManager
import consulo.util.lang.StringUtil
import com.intellij.java.language.psi.{PsiClass, PsiField, PsiMethod}
import consulo.language.psi.{PsiElement, PsiNamedElement}
import com.intellij.java.indexing.search.searches.ClassInheritorsSearch
import consulo.language.psi.util.PsiTreeUtil
import java.util.function.{Function => IJFunction}
import org.jetbrains.plugins.scala.incremental.Highlighting._
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.icons.Icons
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.{ScCaseClauses, ScPattern}
import org.jetbrains.plugins.scala.lang.psi.api.base.{Constructor, ScFieldId, ScReference}
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.api.statements._
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.{ScClassParameter, ScParameter}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScNamedElement
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef._
import org.jetbrains.plugins.scala.lang.psi.types.TermSignature
import org.jetbrains.plugins.scala.lang.psi.impl.search.ScalaExportedMemberUtil
import org.jetbrains.plugins.scala.util.SAMUtil._

import consulo.language.editor.Pass
import consulo.ui.event.ComponentEvent
import java.util.Collections.singletonList
import java.{util => ju}

@ExtensionImpl
final class ScalaLineMarkerProvider extends LineMarkerProviderDescriptor {
  override def getLanguage: Language = ScalaLanguage.INSTANCE


  import Gutter._
  import GutterIconRenderer.Alignment
  import GutterUtil._
  import ScalaMarkerType._

  override def getLineMarkerInfo(element: PsiElement): LineMarkerInfo[? <: PsiElement] = {
    val file = element.getContainingFile
    val project = if (file != null) file.getProject else element.getProject // Avoid tree walk-up

    if (!builtInHighlightingDisabledIn(project) && !element.isVisible(project, file)) return null

    if (element.isValid) {
      val lineMarkerInfo =
        getOverridesImplementsMarkers(element)
          .orElse(companionMarker(element))
          .orNull

      augmentSeparatorInfo(element, lineMarkerInfo)
    }
    else null
  }

  //Method separators can be enabled in
  //File | Settings | Editor | General | Appearance | Show method separators
  private def augmentSeparatorInfo(element: PsiElement, lineMarkerInfo: LineMarkerInfo[? <: PsiElement]): LineMarkerInfo[? <: PsiElement] =
    if (DaemonCodeAnalyzerSettings.getInstance().SHOW_METHOD_SEPARATORS && ScalaMethodSeparatorUtils.isMethodSeparatorNeeded(element)) {
      val info = if (lineMarkerInfo eq null) createMarkerInfo(element) else lineMarkerInfo
      addSeparatorInfo(info)
      info
    } else lineMarkerInfo

  private def createMarkerInfo(element: PsiElement): LineMarkerInfo[PsiElement] = {
    val leaf = PsiTreeUtil.firstChild(element).toOption.getOrElse(element)
    new LineMarkerInfo[PsiElement](
      leaf,
      leaf.getTextRange,
      null,
      Pass.LINE_MARKERS,
      null,
      null,
      GutterIconRenderer.Alignment.RIGHT
    )
  }

  private def addSeparatorInfo(info: LineMarkerInfo[? <: PsiElement]): Unit = {
    val colorScheme = EditorColorsManager.getInstance.getGlobalScheme
    info.separatorColor = colorScheme.getColor(CodeInsightColors.METHOD_SEPARATORS_COLOR)
    info.separatorPlacement = SeparatorPlacement.TOP
  }

  private def arrowUpLineMarker(
    element: PsiElement,
    icon: Image,
    markerType: ScalaMarkerType,
    presentationParent: Option[PsiElement] = None
  ): LineMarkerInfo[PsiElement] = {
    val info = ArrowUpOrDownLineMarkerInfo(element, icon, markerType, Alignment.LEFT, presentationParent)
    NavigateAction.setNavigateAction(info, ScalaBundle.message("go.to.super.method"), IdeActions.ACTION_GOTO_SUPER)
  }

  /* Validates that this psi element can be the first one in a lambda */
  private def canBeFunctionalExpressionAnchor(e: PsiElement): Boolean = {
    val elementType = e.getNode.getElementType
    elementType match {
      case ScalaTokenTypes.tLBRACE =>
        e.getNextSiblingNotWhitespace.isInstanceOf[ScCaseClauses]
      case ScalaTokenTypes.tIDENTIFIER | ScalaTokenTypes.tUNDER | ScalaTokenTypes.tLPARENTHESIS =>
        true
      case _ =>
        false
    }
  }

  private def funExprParent(element: PsiElement): Option[(ScExpression, PsiClass)] =
    element.parentsInFile.collectFirst {
      case _: ScMember                    => None
      case e @ SAMTypeImplementation(sam) => Option(e -> sam)
    }.flatten

  private val trivialSAMs: Set[String] = Set("scala.Function", "scala.PartialFunction", "java.util.function", "scala.ContextFunction")
  private def isInterestingSAM(sam: PsiClass): Boolean = !trivialSAMs.exists(sam.qualifiedName.startsWith)

  private def getImplementsSAMTypeMarker(element: PsiElement): Option[LineMarkerInfo[? <: PsiElement]] = {
    if (!SamOption.isEnabled) {
      return None
    }

    if (canBeFunctionalExpressionAnchor(element)) for {
      (parent, sam) <- funExprParent(element)
      if isInterestingSAM(sam) &&
        PsiTreeUtil.getDeepestFirst(parent) == element
      icon = ImplementingFunctional
      markerType = samTypeImplementation(sam)
    } yield arrowUpLineMarker(element, icon, markerType, Option(parent))
    else None
  }

  private def getOverridesImplementsMarkers(element: PsiElement): Option[LineMarkerInfo[? <: PsiElement]] = {
    val isEnabled = OverridingOption.isEnabled || ImplementingOption.isEnabled
    if (!isEnabled)
      return None

    val isIdentifier = element.getNode.getElementType == ScalaTokenTypes.tIDENTIFIER
    if (isIdentifier) {
      val exportedMemberMarker = lineMarkerForExportedOverride(element)
      exportedMemberMarker.getOrElse {
        val isReference = element.parent.exists(_.is[ScReference])
        if (!isReference)
          getOverridesImplementsMarkersForIdentifier(element)
        else
          None
      }
    } else None
  }

  private def getOverridesImplementsMarkersForIdentifier(element: PsiElement): Option[LineMarkerInfo[PsiElement]] = {
    def containsNamedElement(holder: ScDeclaredElementsHolder): Boolean =
      holder.declaredElements.exists(_.asInstanceOf[ScNamedElement].nameId == element)

    val text = element.getText

    val builtInHighlightingDisabled = builtInHighlightingDisabledIn(element.getProject)
    val parent = namedParent(element) match {
      case Some(value) => value
      case None =>
        return None
    }
    parent match {
      case method: ScFunction if method.isDefinedInClass && method.name == text =>
        if (builtInHighlightingDisabled) return simpleOverrideMarkerFor(method, element)
        val signatures = method.superSignaturesIncludingSelfType
        val icon       = getOverridesOrImplementsIcon(method, signatures)
        if (!isEnabled(icon)) {
          return None
        }
        val markerType = overridingMember
        if (signatures.nonEmpty) arrowUpLineMarker(element, icon, markerType).toOption
        else None
      case cParam: ScClassParameter if cParam.name == text =>
        if (builtInHighlightingDisabled) return simpleOverrideMarkerFor(cParam, element)
        val signatures = ScalaPsiUtil.superValsSignatures(cParam, withSelfType = true)
        val icon       = getOverridesOrImplementsIcon(cParam, signatures)
        if (!isEnabled(icon)) {
          return None
        }
        val markerType = overridingMember
        if (signatures.nonEmpty) arrowUpLineMarker(element, icon, markerType).toOption
        else None
      case v: ScValueOrVariable if !v.isLocal && containsNamedElement(v) =>
        if (builtInHighlightingDisabled) return simpleOverrideMarkerFor(v, element)
        val bindings   = v.declaredElements.filter(e => element.textMatches(e.name))
        val signatures = bindings.flatMap(ScalaPsiUtil.superValsSignatures(_, withSelfType = true))
        val icon       = getOverridesOrImplementsIcon(v, signatures)
        if (!isEnabled(icon)) {
          return None
        }
        val markerType = overridingMember
        if (signatures.nonEmpty) arrowUpLineMarker(element, icon, markerType).toOption
        else None
      case ta: ScTypeAlias if ta.isDefinedInClass && ta.name == text =>
        if (builtInHighlightingDisabled) return simpleOverrideMarkerFor(ta, element)
        val elements = ScalaPsiUtil.superTypeMembers(ta, withSelfType = true)
        val icon = ImplementingMethod
        if (!isEnabled(icon)) {
          return None
        }
        val typez = overridingMember
        if (elements.nonEmpty) arrowUpLineMarker(element, icon, typez).toOption
        else None
      case _ =>
        None
    }
  }

  /**
   * Handle this case: {{{
   *   trait T { def run: Int }
   *   class C extends T {
   *     val delegate: T = ???
   *     export delegate.run // this creates the method "run" which overrides method from "T"
   *   }
   * }}}
   */
  private def lineMarkerForExportedOverride(element: PsiElement): Option[Option[LineMarkerInfo[PsiElement]]] = {
    val exportedMemberOverride = ScalaExportedMemberUtil.exportedMemberOverrideAt(element)
    exportedMemberOverride.map(lineMarkerForExportedOverride(element, _))
  }

  private def lineMarkerForExportedOverride(element: PsiElement, exportedMember: ScalaExportedMemberUtil.ExportedMemberOverride): Option[LineMarkerInfo[PsiElement]] = {
    val icon = getOverridesOrImplementsIcon(exportedMember.semantic, exportedMember.superSignatures)
    if (isEnabled(icon))
      arrowUpLineMarker(element, icon, overridingMember).toOption
    else
      None
  }

  private def simpleOverrideMarkerFor(member: ScMember, element: PsiElement): Option[LineMarkerInfo[PsiElement]] =
    if (isEnabled(OverridingMethod) && member.hasModifierProperty("override")) arrowUpLineMarker(element, OverridingMethod, overridingMember).toOption
    else None

  override def collectSlowLineMarkers(elements: ju.List[? <: PsiElement],
                                      result: ju.Collection[? >: LineMarkerInfo[?]]): Unit = {
    import scala.jdk.CollectionConverters._

    lazy val file = elements.getFirst.getContainingFile
    lazy val project = if (file != null) file.getProject else elements.getFirst.getProject // Avoid tree walk-up

    val markersSam = elements.asScala.filter(_.isValid).filter(_.isVisible(project, file)).flatMap { element =>
      getImplementsSAMTypeMarker(element).map(augmentSeparatorInfo(element, _))
    }
    markersSam.foreach(result.add)

    if (!OverriddenOption.isEnabled && !ImplementedOption.isEnabled) {
      return
    }

    ApplicationManager.getApplication.assertReadAccessAllowed()

    val identifiers = elements.asScala.filter(_.isVisible(project, file)).filter(_.elementType == ScalaTokenTypes.tIDENTIFIER)
    val markers = identifiers.flatMap { identifier =>
      ProgressManager.checkCanceled()
      val context = identifier.parent match {
        case Some(_: ScPattern | _: ScFieldId) => namedParent(identifier)
        case other                             => other
      }

      // Note, this logic is used during line mergers collection
      // When you click on the line marker, a different logic will be used to navigate to the overriding / implementing element
      // See org.jetbrains.plugins.scala.annotator.gutter.ScalaInheritorsLineMarkerNavigator
      context match {
        case Some(tDef: ScTypeDefinition)                      => collectInheritingClassesMarker(tDef)
        case Some(member: ScMember) if member.isDefinedInClass => collectOverriddenMemberMarker(member, identifier)
        case _                                                 => None
      }
    }
    markers.foreach(result.add)
  }

  override def getName: LocalizeValue = LocalizeValue.of(ScalaBundle.message("scala.line.markers"))

  override def getOptions: Array[GutterIconDescriptor.Option] = Options
}

// TODO Split methods between a companion object and packet object (so that methods can be more private)
private object GutterUtil {
  private[gutter] lazy val CompanionOption = new GutterIconDescriptor.Option("scala.companion", LocalizeValue.of(ScalaBundle.message("gutter.companion")), Icons.CLASS_COMPANION)
  private[gutter] val ImplementedOption = new GutterIconDescriptor.Option("scala.implemented", LocalizeValue.of(ScalaBundle.message("gutter.implemented")), AllIcons.Gutter.ImplementedMethod)
  private[gutter] val ImplementingOption = new GutterIconDescriptor.Option("scala.implementing", LocalizeValue.of(ScalaBundle.message("gutter.implementing")), AllIcons.Gutter.ImplementingMethod)
  private[gutter] val SamOption = new GutterIconDescriptor.Option("scala.sam", LocalizeValue.of(ScalaBundle.message("gutter.sam")), AllIcons.Gutter.ImplementingFunctional)
  private[gutter] val OverriddenOption = new GutterIconDescriptor.Option("scala.overridden", LocalizeValue.of(ScalaBundle.message("gutter.overridden")), AllIcons.Gutter.OverridenMethod)
  private[gutter] val OverridingOption = new GutterIconDescriptor.Option("scala.overriding", LocalizeValue.of(ScalaBundle.message("gutter.overriding")), AllIcons.Gutter.OverridingMethod)
  private[gutter] lazy val RecursionOption = new GutterIconDescriptor.Option("scala.recursion", LocalizeValue.of(ScalaBundle.message("gutter.recursion")), Icons.TAIL_RECURSION)

  private[gutter] lazy val Options = Array(
    CompanionOption,
    ImplementedOption,
    ImplementingOption,
    SamOption,
    OverriddenOption,
    OverridingOption,
    RecursionOption,
  )

  // Only for overrid* / implement* icons
  private[gutter] def isEnabled(icon: Image): Boolean =
    OverridingOption.isEnabled && icon == AllIcons.Gutter.OverridingMethod ||
      ImplementingOption.isEnabled && icon == AllIcons.Gutter.ImplementingMethod ||
      OverriddenOption.isEnabled && icon == AllIcons.Gutter.OverridenMethod ||
      ImplementedOption.isEnabled && icon == AllIcons.Gutter.ImplementedMethod

  import Gutter._
  import GutterIconRenderer.Alignment
  import ScalaMarkerType._

  private[gutter] final case class ArrowUpOrDownLineMarkerInfo(
    element:            PsiElement,
    icon:               Image,
    markerType:         ScalaMarkerType,
    alignment: Alignment,
    presentationParent: Option[PsiElement] = None
  ) extends MergeableLineMarkerInfo[PsiElement](
    element,
    element.getTextRange,
    icon,
    Pass.LINE_MARKERS,
    markerType.tooltipProvider,
    markerType.navigationHandler,
    alignment
  ) {
    override def canMergeWith(other: MergeableLineMarkerInfo[?]): Boolean = other match {
      case that: ArrowUpOrDownLineMarkerInfo => icon == that.icon
      case _                                 => false
    }

    override def getCommonIcon(infos: ju.List[MergeableLineMarkerInfo[?]]): Image = icon

    override def getCommonTooltip(infos: ju.List[MergeableLineMarkerInfo[?]]): IJFunction[? >: PsiElement, String] = _ => {
      markerType match {
        case ScalaMarkerType.overriddenMember => ScalaBundle.message("multiple.overriden.tooltip")
        case _                                => ScalaBundle.message("multiple.overriding.tooltip")
      }
    }

    override def getCommonIconAlignment(infos: ju.List[MergeableLineMarkerInfo[?]]): GutterIconRenderer.Alignment =
      infos.get(0) match {
        case that: ArrowUpOrDownLineMarkerInfo =>
          that.alignment
        case _ =>
          super.getCommonIconAlignment(infos)
      }

    override def getElementPresentation(element: PsiElement): String =
      presentationParent.fold(super.getElementPresentation(element))(
        parent => StringUtil.shortenTextWithEllipsis(parent.getText, 100, 0)
      )
  }

  def getOverridesOrImplementsIcon(element: PsiElement, signatures: Seq[TermSignature]): Image =
    if (isOverrides(element, signatures.map(_.namedElement))) OverridingMethod
    else ImplementingMethod

  def namedParent(e: PsiElement): Option[PsiElement] =
    e.withParentsInFile.find(ScalaPsiUtil.isNameContext)

  def collectInheritingClassesMarker(aClass: ScTypeDefinition): Option[LineMarkerInfo[? <: PsiElement]] = {
    val inheritor = ClassInheritorsSearch.search(aClass, false).findFirst.toOption
    inheritor.map { _ =>
      val range = aClass.nameId.getTextRange

      val icon = aClass match {
        case _: ScTrait => ImplementedMethod
        case _ => OverridenMethod
      }

      if (!isEnabled(icon)) {
        return None
      }

      val info = new LineMarkerInfo(
        aClass.nameId,
        range,
        icon,
        Pass.LINE_MARKERS,
        subclassedClass.tooltipProvider,
        subclassedClass.navigationHandler,
        Alignment.RIGHT
      )
      NavigateAction.setNavigateAction(info, ScalaBundle.message("go.to.implementation"), IdeActions.ACTION_GOTO_IMPLEMENTATION)
    }
  }

  def collectOverriddenMemberMarker(member: ScMember, anchor: PsiElement): Option[LineMarkerInfo[? <: PsiElement]] =
    member match {
      case Constructor(_) => None
      case _ =>
        val icon = if (isAbstract(member)) ImplementedMethod else OverridenMethod

        if (!isEnabled(icon)) {
          return None
        }

        val overrides = ScalaMarkerType.findOverrides(member, deep = false)
        if (overrides.isEmpty) None else {
          val info = ArrowUpOrDownLineMarkerInfo(
            anchor,
            icon,
            overriddenMember,
            Alignment.RIGHT
          )
          Some(NavigateAction.setNavigateAction(info, ScalaBundle.message("go.to.super.method"), IdeActions.ACTION_GOTO_SUPER))
        }
    }

  def isOverrides(element: PsiElement, supers: Seq[PsiNamedElement]): Boolean =
    element match {
      case _: ScFunctionDeclaration  => true
      case _: ScValueDeclaration     => true
      case _: ScVariableDeclaration  => true
      case _: ScTypeAliasDeclaration => true
      case _ =>
        val iter = supers.iterator
        while (iter.hasNext) {
          val named = iter.next()
          named.nameContext match {
            case _: ScFunctionDefinition                          => return true
            case _: ScFunction                                    =>
            case method: PsiMethod if !method.hasAbstractModifier => return true
            case _: ScVariableDefinition | _: ScPatternDefinition => return true
            case f: PsiField if !f.hasAbstractModifier            => return true
            case _: ScVariableDeclaration                         =>
            case _: ScValueDeclaration                            =>
            case _: ScParameter                                   => return true
            case _: ScTypeAliasDefinition                         => return true
            case _: ScTypeAliasDeclaration                        =>
            case _: PsiClass                                      => return true
            case _                                                =>
          }
        }
        false
    }

  def isAbstract(element: PsiElement): Boolean = element match {
    case _: ScFunctionDeclaration  => true
    case _: ScValueDeclaration     => true
    case _: ScVariableDeclaration  => true
    case _: ScTypeAliasDeclaration => true
    case _                         => false
  }

  // Show companion for class / trait / object / enum in the gutter, https://youtrack.jetbrains.com/issue/SCL-17697
  private[gutter] def companionMarker(element: PsiElement): Option[LineMarkerInfo[? <: PsiElement]] = {
    if (!CompanionOption.isEnabled) {
      return None
    }

    // TODO Enable in tests when GutterMarkersTest will be able to separate different maker providers
    if (ApplicationManager.getApplication.isUnitTestMode) None
    else element match {
      case identifier @ ElementType(ScalaTokenTypes.tIDENTIFIER) & Parent(typeDefinition: ScTypeDefinitionLike) =>
        typeDefinition.baseCompanion.map { companion =>
          val swapped = typeDefinition.startOffset > companion.startOffset ^ typeDefinition.is[ScObject]

          val info = new LineMarkerInfo(identifier,
            identifier.getTextRange,
            iconFor(typeDefinition, swapped),
            Pass.LINE_MARKERS,
            (_: PsiElement) =>
              GutterTooltipHelper.getTooltipText(singletonList(companion.nameId.getPrevSiblingNotWhitespace), ScalaBundle.message("has.companion", nameOf(companion)), false, IdeActions.ACTION_GOTO_DECLARATION)
                .replace("to navigate", "on the keyword to navigate"), // Not internationalizable (which is somewhat OK).
            (_: ComponentEvent[?], _: PsiElement) =>
              Option(PsiNavigationSupport.getInstance.createNavigatable(companion.getProject,
                companion.getContainingFile.getVirtualFile, companion.nameId.getPrevSiblingNotWhitespace.startOffset)).foreach(_.navigate(true)),
            Alignment.LEFT
          )
          NavigateAction.setNavigateAction(info, ScalaBundle.message("go.to.companion", nameOf(companion)), IdeActions.ACTION_GOTO_DECLARATION)

        }

      case _ => None
    }
  }

  private def nameOf(definition: ScTypeDefinitionLike) = definition match {
    case _: ScEnum => ScalaBundle.message("companion.enum")
    case _: ScClass => ScalaBundle.message("companion.class")
    case _: ScTrait => ScalaBundle.message("companion.trait")
    case _: ScTypeAlias => ScalaBundle.message("companion.type")
    case _: ScObject => ScalaBundle.message("companion.object")
    case _ => "" // Just "Has a companion" is OK.
  }

  private def iconFor(definition: ScTypeDefinitionLike, swapped: Boolean): Image = definition match {
    case _: ScEnum => if (swapped) Icons.CLASS_COMPANION_SWAPPED else Icons.CLASS_COMPANION
    case _: ScClass => if (swapped) Icons.CLASS_COMPANION_SWAPPED else Icons.CLASS_COMPANION
    case _: ScTrait => if (swapped) Icons.TRAIT_COMPANION_SWAPPED else Icons.TRAIT_COMPANION
    case _: ScTypeAlias => if (swapped) Icons.TYPE_COMPANION_SWAPPED else Icons.TYPE_COMPANION
    case _: ScObject => if (swapped) Icons.OBJECT_COMPANION_SWAPPED else Icons.OBJECT_COMPANION
    case _ => null
  }
}
