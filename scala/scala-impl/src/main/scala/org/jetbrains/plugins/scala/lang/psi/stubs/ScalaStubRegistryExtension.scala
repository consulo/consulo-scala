package org.jetbrains.plugins.scala.lang.psi.stubs

import consulo.annotation.component.ExtensionImpl
import consulo.language.ast.IElementType
import consulo.language.psi.stub.{ObjectStubSerializerProvider, StubElementTypeHolder}
import org.jetbrains.plugins.scala.lang.parser.{Scala3ParserDefinition, ScalaElementType, ScalaParserDefinition}
import org.jetbrains.plugins.scala.lang.psi.stubs.factories._
import org.jetbrains.plugins.scala.lang.psi.stubs.factories.signatures._

import java.util

/**
 * upstream: StubRegistryExtension registering stub serializing factories for plain element types.
 * In Consulo Scala stub element types are [[consulo.language.psi.stub.IStubElementType]]s registered on
 * creation; this holder only makes sure they are created before the stub index is initialized.
 */
@ExtensionImpl
final class ScalaStubRegistryExtension extends StubElementTypeHolder[AnyRef] {
  override def getExternalIdPrefix: String = null

  override def loadSerializers(): util.List[ObjectStubSerializerProvider] = {
    ScalaParserDefinition.FileNodeType
    Scala3ParserDefinition.FileNodeType
    ScalaStubRegistryExtension.factories
    util.List.of()
  }
}

object ScalaStubRegistryExtension {

  private[stubs] lazy val factories: Map[IElementType, ScStubSerializingElementFactory[?, ?]] =
    Map[IElementType, ScStubSerializingElementFactory[?, ?]](
      ScalaElementType.MODIFIERS -> new ScModifiersStubFactory(ScalaElementType.MODIFIERS),
      ScalaElementType.SELF_TYPE -> new ScSelfTypeElementStubFactory(ScalaElementType.SELF_TYPE),
      ScalaElementType.TYPE_DECLARATION -> new ScTypeAliasDeclarationStubFactory(ScalaElementType.TYPE_DECLARATION),
      ScalaElementType.TYPE_DEFINITION -> new ScTypeAliasDefinitionStubFactory(ScalaElementType.TYPE_DEFINITION),
      ScalaElementType.ANNOTATION -> new ScAnnotationStubFactory(ScalaElementType.ANNOTATION),
      ScalaElementType.IMPORT_EXPR -> new ScImportExprStubFactory(ScalaElementType.IMPORT_EXPR),
      ScalaElementType.IMPORT_SELECTOR -> new ScImportSelectorStubFactory(ScalaElementType.IMPORT_SELECTOR),
      ScalaElementType.IMPORT_SELECTORS -> new ScImportSelectorsStubFactory(ScalaElementType.IMPORT_SELECTORS),
      ScalaElementType.DERIVES_CLAUSE -> new ScDerivesClauseStubFactory(ScalaElementType.DERIVES_CLAUSE),
      ScalaElementType.PATTERN_LIST -> new ScPatternListStubFactory(ScalaElementType.PATTERN_LIST),
      ScalaElementType.ANNOTATIONS -> new ScAnnotationsStubFactory(ScalaElementType.ANNOTATIONS),
      ScalaElementType.TEMPLATE_BODY -> new ScTemplateBodyStubFactory(ScalaElementType.TEMPLATE_BODY),
      ScalaElementType.EXTENSION_BODY -> new ScExtensionBodyStubFactory(ScalaElementType.EXTENSION_BODY),
      ScalaElementType.TEMPLATE_PARENTS -> new ScTemplateParentsStubFactory(ScalaElementType.TEMPLATE_PARENTS),
      ScalaElementType.FIELD_ID -> new ScFieldIdStubFactory(ScalaElementType.FIELD_ID),
      ScalaElementType.EARLY_DEFINITIONS -> new ScEarlyDefinitionsStubFactory(ScalaElementType.EARLY_DEFINITIONS),
      ScalaElementType.EXTENDS_BLOCK -> new ScExtendsBlockStubFactory(ScalaElementType.EXTENDS_BLOCK),
      ScalaElementType.EXTENSION -> new ScExtensionStubFactory(ScalaElementType.EXTENSION),
      ScalaElementType.IDENTIFIER_LIST -> new ScIdListStubFactory(ScalaElementType.IDENTIFIER_LIST),
      ScalaElementType.PACKAGING -> new ScPackagingStubFactory(ScalaElementType.PACKAGING),
      ScalaElementType.REFERENCE_PATTERN -> new ScReferencePatternStubFactory(ScalaElementType.REFERENCE_PATTERN),
      ScalaElementType.TYPED_PATTERN -> new ScTypedPatternStubFactory(ScalaElementType.TYPED_PATTERN),
      ScalaElementType.NAMING_PATTERN -> new ScNamingPatternStubFactory(ScalaElementType.NAMING_PATTERN),
      ScalaElementType.SEQ_WILDCARD_PATTERN -> new ScSeqWildcardPatternStubFactory(ScalaElementType.SEQ_WILDCARD_PATTERN),
      ScalaElementType.FUNCTION_DECLARATION -> new ScFunctionDeclarationStubFactory(ScalaElementType.FUNCTION_DECLARATION),
      ScalaElementType.FUNCTION_DEFINITION -> new ScFunctionDefinitionStubFactory(ScalaElementType.FUNCTION_DEFINITION),
      ScalaElementType.MACRO_DEFINITION -> new ScMacroDefinitionStubFactory(ScalaElementType.MACRO_DEFINITION),
      ScalaElementType.GIVEN_ALIAS_DECLARATION -> new ScGivenAliasDeclarationStubFactory(ScalaElementType.GIVEN_ALIAS_DECLARATION),
      ScalaElementType.GIVEN_ALIAS_DEFINITION -> new ScGivenAliasDefinitionStubFactory(ScalaElementType.GIVEN_ALIAS_DEFINITION),
      ScalaElementType.VALUE_DECLARATION -> new ScValueDeclarationStubFactory(ScalaElementType.VALUE_DECLARATION),
      ScalaElementType.PATTERN_DEFINITION -> new ScValueDefinitionStubFactory(ScalaElementType.PATTERN_DEFINITION),
      ScalaElementType.VARIABLE_DECLARATION -> new ScVariableDeclarationStubFactory(ScalaElementType.VARIABLE_DECLARATION),
      ScalaElementType.VARIABLE_DEFINITION -> new ScVariableDefinitionStubFactory(ScalaElementType.VARIABLE_DEFINITION),
      ScalaElementType.ACCESS_MODIFIER -> new ScAccessModifierStubFactory(ScalaElementType.ACCESS_MODIFIER),
      ScalaElementType.ImportStatement -> new ScImportStmtStubFactory(ScalaElementType.ImportStatement),
      ScalaElementType.ExportStatement -> new ScExportStmtStubFactory(ScalaElementType.ExportStatement),
      ScalaElementType.EnumCases -> new ScEnumCasesStubFactory(ScalaElementType.EnumCases),
      ScalaElementType.PRIMARY_CONSTRUCTOR -> new ScPrimaryConstructorStubFactory(ScalaElementType.PRIMARY_CONSTRUCTOR),
      ScalaElementType.PARAM -> new ScParameterStubFactory(ScalaElementType.PARAM),
      ScalaElementType.CLASS_PARAM -> new ScClassParameterStubFactory(ScalaElementType.CLASS_PARAM),
      ScalaElementType.PARAM_CLAUSE -> new ScParamClauseStubFactory(ScalaElementType.PARAM_CLAUSE),
      ScalaElementType.PARAM_CLAUSES -> new ScParamClausesStubFactory(ScalaElementType.PARAM_CLAUSES),
      ScalaElementType.TYPE_PARAM -> new ScTypeParamStubFactory(ScalaElementType.TYPE_PARAM),
      ScalaElementType.TYPE_PARAM_CLAUSE -> new ScTypeParamClauseStubFactory(ScalaElementType.TYPE_PARAM_CLAUSE),
      ScalaElementType.ClassDefinition -> new ScClassDefinitionStubFactory(ScalaElementType.ClassDefinition),
      ScalaElementType.TraitDefinition -> new ScTraitDefinitionStubFactory(ScalaElementType.TraitDefinition),
      ScalaElementType.ObjectDefinition -> new ScObjectDefinitionStubFactory(ScalaElementType.ObjectDefinition),
      ScalaElementType.EnumDefinition -> new ScEnumDefinitionStubFactory(ScalaElementType.EnumDefinition),
      ScalaElementType.EnumClassCase -> new ScEnumClassCaseStubFactory(ScalaElementType.EnumClassCase),
      ScalaElementType.EnumSingletonCase -> new ScEnumSingletonCaseStubFactory(ScalaElementType.EnumSingletonCase),
      ScalaElementType.NewTemplate -> new ScNewTemplateDefinitionStubFactory(ScalaElementType.NewTemplate),
      ScalaElementType.GivenDefinition -> new ScGivenDefinitionStubFactory(ScalaElementType.GivenDefinition)
    )

  def factoryFor(elementType: IElementType): ScStubSerializingElementFactory[?, ?] =
    factories.getOrElse(elementType, throw new IllegalStateException(s"No stub factory registered for $elementType"))
}
