package org.jetbrains.plugins.scala.lang.surroundWith.surrounders.expression

import consulo.localize.LocalizeValue

import consulo.language.psi.PsiElement

class ScalaWithTryCatchFinallySurrounder extends ScalaWithTryCatchSurrounderBase {
  override def getTemplateAsString(elements: Array[PsiElement]): String =
    "try {\n" +
      super.getTemplateAsString(elements) +
      s"\n} catch {\n case _ ${arrow(elements)} \n} finally {}"

  //noinspection ScalaExtractStringToBundle,DialogTitleCapitalization
  override def getTemplateDescription: LocalizeValue = LocalizeValue.of("try / catch / finally")
}
