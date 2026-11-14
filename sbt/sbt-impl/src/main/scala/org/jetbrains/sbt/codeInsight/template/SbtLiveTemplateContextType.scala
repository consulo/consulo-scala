package org.jetbrains.sbt
package codeInsight
package template

import consulo.annotation.component.ExtensionImpl

import com.intellij.codeInsight.template.FileTypeBasedContextType

@ExtensionImpl
final class SbtLiveTemplateContextType extends FileTypeBasedContextType(Sbt.Name, language.SbtFileType)
