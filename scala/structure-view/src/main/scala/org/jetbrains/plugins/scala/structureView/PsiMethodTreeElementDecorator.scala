package org.jetbrains.plugins.scala.structureView

import com.intellij.java.impl.ide.structureView.impl.java.PsiMethodTreeElement
import com.intellij.java.language.psi.PsiMethod
import consulo.localize.LocalizeValue

private class PsiMethodTreeElementDecorator(method: PsiMethod, inherited: Boolean) extends PsiMethodTreeElement(method, inherited) {
  override def getPresentableText: LocalizeValue =
    LocalizeValue.of(PsiMethodTreeElementDecorator.asScalaType(super.getPresentableText.get))
}

private object PsiMethodTreeElementDecorator {
  private val PrimitiveType = "\\b(?:boolean|byte|char|short|int|long|float|double)\\b".r

  private val Transformations = Seq(
    "\\bvoid\\b".r -> "Unit",
    "(\\w+)\\[\\]".r -> "Array[$1]",
    "\\.\\.\\.".r -> "*",
    "\\<".r -> "[",
    "\\>".r -> "]",
    "\\?".r -> "_",
    "\\bextends\\b".r -> "<:",
    "\\bsuper\\b".r -> ">:")

  private def asScalaType(javaType: String) =
    Transformations.foldLeft(PrimitiveType.replaceAllIn(javaType, _.matched.capitalize)) {
      case (acc, (regex, replacement)) => regex.replaceAllIn(acc, replacement)
    }
}
