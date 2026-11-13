package org.jetbrains.plugins.scala

import consulo.language.editor.generation.ClassMemberWithElement
import com.intellij.java.language.impl.codeInsight.generation.PsiElementClassMember
import com.intellij.java.language.psi.PsiDocCommentOwner

package object overrideImplement {
  type ClassMember = PsiElementClassMember[? <: PsiDocCommentOwner] & ScalaNamedMember
  type ClassMember0 = ClassMemberWithElement & ScalaMember
}
