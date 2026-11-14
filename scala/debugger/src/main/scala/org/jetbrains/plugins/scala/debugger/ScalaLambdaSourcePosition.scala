package org.jetbrains.plugins.scala.debugger

import com.intellij.debugger.SourcePosition
import consulo.language.psi.PsiElement

private final class ScalaLambdaSourcePosition(delegate: SourcePosition, val lambda: PsiElement)
  extends AbstractScalaSourcePosition(delegate)
