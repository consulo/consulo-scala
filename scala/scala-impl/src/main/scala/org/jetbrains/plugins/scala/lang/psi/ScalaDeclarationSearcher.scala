package org.jetbrains.plugins.scala.lang.psi

import com.intellij.java.language.jvm.JvmElement
import com.intellij.lang.jvm.source.JvmDeclarationSearcher
import consulo.language.psi.PsiElement

import java.util.Collection
import java.util.Collections.{emptyList, singletonList}

class ScalaDeclarationSearcher extends JvmDeclarationSearcher {
  override def findDeclarations(declaringElement: PsiElement): Collection[JvmElement] =
    declaringElement match {
      case element: JvmElement => singletonList(element)
      case _ => emptyList
    }
}
