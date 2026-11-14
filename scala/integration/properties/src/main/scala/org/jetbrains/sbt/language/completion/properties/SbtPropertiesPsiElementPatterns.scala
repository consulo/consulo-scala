package org.jetbrains.sbt.language.completion.properties

import consulo.language.editor.completion.CompletionInitializationContext
import com.intellij.lang.properties.PropertiesFileType
import com.intellij.lang.properties.psi.Property
import consulo.language.pattern.PlatformPatterns.{psiElement, psiFile}
import consulo.language.pattern.PsiElementPattern.Capture
import consulo.language.pattern.StandardPatterns.instanceOf
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.lang.completion.condition

object SbtPropertiesPsiElementPatterns:
  def propertiesFilePattern: Capture[PsiElement] = psiElement.inFile:
    psiFile.withFileType(instanceOf(classOf[PropertiesFileType]))

  def versionPropertyPattern: Capture[PsiElement] = psiElement().`with`(
    condition[PsiElement]("isVersionPropertyPattern"):
      case property: Property =>
        property.getKey == "sbt.version" && property.getValue.contains(CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED)
      case _ => false
  )
