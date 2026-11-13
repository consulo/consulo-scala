package org.jetbrains.plugins.scala.lang.psi.light

import consulo.language.Language
import com.intellij.java.language.impl.psi.impl.light.LightParameterListBuilder
import consulo.language.psi.PsiManager
import com.intellij.java.language.psi.PsiParameter

private class ScLightParameterList(manager: PsiManager, language: Language, params: Seq[PsiParameter])
  extends LightParameterListBuilder(manager, language) {

  params.foreach(addParameter)
}