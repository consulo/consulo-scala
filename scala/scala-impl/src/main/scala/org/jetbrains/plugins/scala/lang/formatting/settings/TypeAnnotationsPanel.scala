package org.jetbrains.plugins.scala.lang.formatting.settings

import consulo.language.editor.DaemonCodeAnalyzer
import consulo.project.Project
import consulo.ui.{Component, TextBoxWithExpandAction}
import consulo.language.codeStyle.CodeStyleSettings
import consulo.process.cmd.ParametersListUtil
import org.jetbrains.plugins.scala.api.NlsString
import org.jetbrains.plugins.scala.extensions.Binding

import java.util

final class TypeAnnotationsPanel(settings: CodeStyleSettings) extends TypeAnnotationsPanelBase(settings) {

  import TypeAnnotationsPanel._

  private def bindingsFor(settings: CodeStyleSettings): Seq[Binding[?]] = {
    val scalaSettings = settings.getCustomSettings(classOf[ScalaCodeStyleSettings])

    import scalaSettings._

    Seq(
      new Binding(
        TYPE_ANNOTATION_PUBLIC_MEMBER, myPublicMember.getValue.booleanValue)
      (TYPE_ANNOTATION_PUBLIC_MEMBER = _, myPublicMember.setValue(_)),
      new Binding(
        TYPE_ANNOTATION_PROTECTED_MEMBER, myProtectedMember.getValue.booleanValue)
      (TYPE_ANNOTATION_PROTECTED_MEMBER = _, myProtectedMember.setValue(_)),
      new Binding(
        TYPE_ANNOTATION_PRIVATE_MEMBER, myPrivateMember.getValue.booleanValue)
      (TYPE_ANNOTATION_PRIVATE_MEMBER = _, myPrivateMember.setValue(_)),
      new Binding(
        TYPE_ANNOTATION_LOCAL_DEFINITION, myLocalDefinition.getValue.booleanValue)
      (TYPE_ANNOTATION_LOCAL_DEFINITION = _, myLocalDefinition.setValue(_)),
      new Binding(
        TYPE_ANNOTATION_FUNCTION_PARAMETER, myFunctionParameter.getValue.booleanValue)
      (TYPE_ANNOTATION_FUNCTION_PARAMETER = _, myFunctionParameter.setValue(_)),
      new Binding(
        TYPE_ANNOTATION_UNDERSCORE_PARAMETER, myUnderscoerParameter.getValue.booleanValue)
      (TYPE_ANNOTATION_UNDERSCORE_PARAMETER = _, myUnderscoerParameter.setValue(_)),

      new Binding(
        TYPE_ANNOTATION_IMPLICIT_MODIFIER, myImplicitModifier.getValue.booleanValue)
      (TYPE_ANNOTATION_IMPLICIT_MODIFIER = _, myImplicitModifier.setValue(_)),
      new Binding(
        TYPE_ANNOTATION_UNIT_TYPE, myUnitType.getValue.booleanValue)
      (TYPE_ANNOTATION_UNIT_TYPE = _, myUnitType.setValue(_)),
      new Binding(
        TYPE_ANNOTATION_STRUCTURAL_TYPE, myStructuralType.getValue.booleanValue)
      (TYPE_ANNOTATION_STRUCTURAL_TYPE = _, myStructuralType.setValue(_)),

      new Binding(
        TYPE_ANNOTATION_EXCLUDE_MEMBER_OF_ANONYMOUS_CLASS, myMemberOfAnonymousClass.getValue.booleanValue)
      (TYPE_ANNOTATION_EXCLUDE_MEMBER_OF_ANONYMOUS_CLASS = _, myMemberOfAnonymousClass.setValue(_)),
      new Binding(
        TYPE_ANNOTATION_EXCLUDE_MEMBER_OF_PRIVATE_CLASS, myMemberOfPrivateClass.getValue.booleanValue)
      (TYPE_ANNOTATION_EXCLUDE_MEMBER_OF_PRIVATE_CLASS = _, myMemberOfPrivateClass.setValue(_)),
      new Binding(
        TYPE_ANNOTATION_EXCLUDE_CONSTANT, myConstant.getValue.booleanValue)
      (TYPE_ANNOTATION_EXCLUDE_CONSTANT = _, myConstant.setValue(_)),
      new Binding(
        TYPE_ANNOTATION_EXCLUDE_WHEN_TYPE_IS_STABLE, myStableType.getValue.booleanValue)
      (TYPE_ANNOTATION_EXCLUDE_WHEN_TYPE_IS_STABLE = _, myStableType.setValue(_)),
      new Binding(
        TYPE_ANNOTATION_EXCLUDE_IN_TEST_SOURCES, myTestSources.getValue.booleanValue)
      (TYPE_ANNOTATION_EXCLUDE_IN_TEST_SOURCES = _, myTestSources.setValue(_)),
      new Binding(
        TYPE_ANNOTATION_EXCLUDE_IN_DIALECT_SOURCES, myDialectSources.getValue.booleanValue)
      (TYPE_ANNOTATION_EXCLUDE_IN_DIALECT_SOURCES = _, myDialectSources.setValue(_)),

      new Binding(
        TYPE_ANNOTATION_EXCLUDE_MEMBER_OF, elementsIn(myMembers))
      (TYPE_ANNOTATION_EXCLUDE_MEMBER_OF = _, setElementsIn(myMembers, _)),
      new Binding(
        TYPE_ANNOTATION_EXCLUDE_ANNOTATED_WITH, elementsIn(myAnnotations))
      (TYPE_ANNOTATION_EXCLUDE_ANNOTATED_WITH = _, setElementsIn(myAnnotations, _)),
      new Binding(
        TYPE_ANNOTATION_EXCLUDE_WHEN_TYPE_MATCHES, elementsIn(myTypes))
      (TYPE_ANNOTATION_EXCLUDE_WHEN_TYPE_MATCHES = _, setElementsIn(myTypes, _))
    )
  }

  private var projectOpt: Option[Project] = None

  def onProjectSet(aProject: Project): Unit = {
    projectOpt = Some(aProject)
  }

  override def dispose(): Unit = {
    projectOpt = None
    super.dispose()
  }

  override protected def getPanelInner: Component = myContent

  override def isModified(settings: CodeStyleSettings): Boolean =
    bindingsFor(settings).exists(!_.leftEqualsRight)

  override protected def resetImpl(settings: CodeStyleSettings): Unit =
    bindingsFor(settings).foreach(it => it.copyLeftToRight())

  override def apply(settings: CodeStyleSettings): Unit = {
    bindingsFor(settings).foreach(it => it.copyRightToLeft())

    projectOpt.foreach { project =>
      DaemonCodeAnalyzer.getInstance(project).restart("Restart after applying changes from TypeAnnotationsPanel")
    }
  }
}

object TypeAnnotationsPanel {
  private def elementsIn(field: TextBoxWithExpandAction): util.Set[String] =
    new util.HashSet(ParametersListUtil.COLON_LINE_PARSER.apply(field.getValue))

  private def setElementsIn(field: TextBoxWithExpandAction, elements: util.Set[String]): Unit =
    field.setValue(NlsString.force(ParametersListUtil.COLON_LINE_JOINER.apply(new util.ArrayList(elements))))
}
