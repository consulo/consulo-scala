package org.jetbrains.plugins.scala.codeInspection.shadow

import consulo.language.editor.intention.{HighPriorityAction, LowPriorityAction}
import consulo.language.editor.inspection.scheme.InspectionManager
import consulo.configurable.{ConfigurableBuilder, UnnamedConfigurable}
import consulo.language.editor.inspection.{InspectionToolState, LocalInspectionTool, LocalInspectionToolSession, LocalQuickFix, ProblemDescriptor, ProblemHighlightType, ProblemsHolder, UpdateInspectionOptionFix}
import consulo.localize.LocalizeValue
import consulo.util.xml.serializer.XmlSerializerUtil
import consulo.language.psi.scope.LocalSearchScope
import consulo.language.psi.search.ReferencesSearch
import consulo.language.psi.util.PsiTreeUtil
import consulo.language.psi.{PsiElement, PsiElementVisitor}
import org.jetbrains.annotations.{Nls, NonNls}
import org.jetbrains.plugins.scala.incremental.Highlighting._
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.codeInspection.quickfix.RenameElementQuickfix
import org.jetbrains.plugins.scala.codeInspection.ui.CompilerInspectionOptions._
import org.jetbrains.plugins.scala.lang.lexer.ScalaModifier
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScVariable
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScClassParameter
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.{ScModifierListOwner, ScNamedElement}
import org.jetbrains.plugins.scala.util.EnumSet.EnumSetOps

import scala.beans.BooleanBeanProperty
import scala.collection.mutable

final class PrivateShadowInspection extends LocalInspectionTool {

  import PrivateShadowInspection._

  override def createStateProvider(): InspectionToolState[?] = new PrivateShadowInspectionState

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession, state: Object): PsiElementVisitor =
    buildVisitor(holder, isOnTheFly, state.asInstanceOf[PrivateShadowInspectionState])

  private def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, state: PrivateShadowInspectionState): PsiElementVisitor = new PsiElementVisitor {
    import state._

    override def visitElement(element: PsiElement): Unit = {
      if (!element.isVisible(holder.getProject, holder.getFile)) return

      element match {
        case elem: ScNamedElement if
          isInspectionAllowed(elem, privateShadowCompilerOption, "-Xlint:private-shadow") &&
            isClassParamWithoutAccessModsAndOverride(elem) && isElementShadowing(elem) =>
          holder.registerProblem(createProblemDescriptor(elem, annotationDescription, state)(using holder.getManager, isOnTheFly))
        case _ =>
      }
    }
  }

  private def createProblemDescriptor(elem: ScNamedElement, @Nls description: String, state: PrivateShadowInspectionState)
                                     (implicit manager: InspectionManager, isOnTheFly: Boolean): ProblemDescriptor = {
    import state._
    val showAsError =
      fatalWarningsCompilerOption &&
        (isCompilerOptionPresent(elem, "-Xfatal-warnings") || isCompilerOptionPresent(elem, "-Werror"))

    val fixes = mutable.ArrayBuilder.make[LocalQuickFix]
    fixes.addOne(new RenameElementQuickfix(elem, renameQuickFixDescription) with HighPriorityAction)
    // upstream: DisableInspectionToolAction - Consulo offers disabling of inspection itself
    if (!privateShadowCompilerOption)
      fixes.addOne(new UpdateInspectionOptionFix[PrivateShadowInspection, PrivateShadowInspectionState](this, LocalizeValue.of(ScalaInspectionBundle.message("fix.private.shadow.compiler.option.label")), _.privateShadowCompilerOption = true))
    if (!fatalWarningsCompilerOption)
      fixes.addOne(new UpdateInspectionOptionFix[PrivateShadowInspection, PrivateShadowInspectionState](this, LocalizeValue.of(ScalaInspectionBundle.message("fix.private.shadow.fatal.warnings.label")), _.fatalWarningsCompilerOption = true))
    manager.createProblemDescriptor(
      elem.nameId,
      description,
      isOnTheFly,
      fixes.result(),
      if (showAsError) ProblemHighlightType.GENERIC_ERROR else ProblemHighlightType.GENERIC_ERROR_OR_WARNING
    )
  }

  private def isClassParamWithoutAccessModsAndOverride(elem: ScNamedElement): Boolean =
    elem.nameContext match {
      case e: ScModifierListOwner if e.getModifierList.modifiers.contains(ScalaModifier.Override) => false
      case p: ScClassParameter if p.getModifierList.accessModifier.isEmpty                        => true
      case _                                                                                      => false
    }

  private def isElementShadowing(elem: ScNamedElement): Boolean =
    // Fields suspected of being shadowed are all fields belonging to the containing class or trait with the same name
    // as the element under inspection, but not itself.
    Option(PsiTreeUtil.getParentOfType(elem, classOf[ScTypeDefinition])).exists { typeDefinition =>
      typeDefinition
        .allTermsByName(elem.name)
        .exists {
          case term: ScNamedElement =>
            lazy val isUsed =
              typeDefinition.extendsBlock.templateBody.exists { body =>
                val scope = new LocalSearchScope(body)
                ReferencesSearch.search(elem, scope).findFirst() != null
              }
            term.nameContext match {
              case s: ScVariable if !s.isPrivate && !s.isAbstract => isUsed
              case s: ScClassParameter if s.isVar && !s.isPrivate => isUsed
              case _ => false
            }
          case _ => false
        }
    }
}

private[shadow] object PrivateShadowInspection {
  @Nls
  val annotationDescription: String = ScalaInspectionBundle.message("private.shadow.description")

  @Nls
  private val renameQuickFixDescription: String = ScalaInspectionBundle.message("private.shadow.rename.identifier")
}

final class PrivateShadowInspectionState extends InspectionToolState[PrivateShadowInspectionState] {
  @BooleanBeanProperty var privateShadowCompilerOption: Boolean = true
  @BooleanBeanProperty var fatalWarningsCompilerOption: Boolean = true

  override def createConfigurable(): UnnamedConfigurable =
    ConfigurableBuilder.newBuilder()
      .checkBox(LocalizeValue.of(ScalaInspectionBundle.message("private.shadow.compiler.option.label")), () => privateShadowCompilerOption, privateShadowCompilerOption = _)
      .checkBox(LocalizeValue.of(ScalaInspectionBundle.message("private.shadow.fatal.warnings.label")), () => fatalWarningsCompilerOption, fatalWarningsCompilerOption = _)
      .buildUnnamed()

  override def getState: PrivateShadowInspectionState = this

  override def loadState(state: PrivateShadowInspectionState): Unit = XmlSerializerUtil.copyBean(state, this)
}
