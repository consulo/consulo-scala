package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.stub.IStubElementType
import consulo.language.psi.PsiElement
import consulo.language.psi.stub.StubElement
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTemplateDefinition
import org.jetbrains.plugins.scala.lang.psi.stubs.ScTemplateDefinitionStub

class ScTemplateDefinitionStubImpl[TypeDef <: ScTemplateDefinition](
  parent:                                             StubElement[? <: PsiElement],
  elementType:                                        IStubElementType[?, ?],
  nameRef:                                            String,
  override val getQualifiedName:                      String,
  override val getSourceFileName:                     String,
  override val javaName:                              String,
  override val javaQualifiedName:                     String,
  override val additionalJavaName:                    Option[String],
  override val isPackageObject:                       Boolean,
  override val isDeprecated:                          Boolean,
  override val isLocal:                               Boolean,
  override val isVisibleInJava:                       Boolean,
  override val isImplicitObject:                      Boolean,
  override val implicitConversionParameterClass:      Option[String],
  override val implicitClassNames:                    Array[String],
  override val isTopLevel:                            Boolean,
  override val topLevelQualifier:                     Option[String],
  override val isGiven:                               Boolean,
  override val givenClassNames:                       Array[String],
  override val givenDefinitionParameterText:          String,
  override val enumClassCaseMentionsParentTypeParams: Boolean,
) extends ScNamedStubBase[TypeDef](parent, elementType, nameRef)
    with ScTemplateDefinitionStub[TypeDef] {

  //todo PsiClassStub methods
  // upstream: override def getLanguageLevel = LanguageLevel.JDK_1_5 (Consulo PsiClassStub has no getLanguageLevel)

  override def isEnum: Boolean = false

  override def isInterface: Boolean = false

  override def isAnonymous: Boolean = false

  override def isAnonymousInQualifiedNew: Boolean = false

  override def isAnnotationType: Boolean = false

  override def hasDeprecatedAnnotation: Boolean = false

  override def isEnumConstantInitializer: Boolean = false

  override def getBaseClassReferenceText: String = null
}
