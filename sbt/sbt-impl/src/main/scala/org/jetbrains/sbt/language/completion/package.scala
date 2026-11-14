package org.jetbrains.sbt
package language


import org.jetbrains.sbt.api.language.SbtFileType
import consulo.language.pattern.PlatformPatterns.{psiElement, psiFile}
import consulo.language.pattern.PsiElementPattern.Capture
import consulo.language.pattern.StandardPatterns.instanceOf
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScInfixExpr

package object completion {

  val MODULE_ID_OPS = List("%", "%%")
  val SBT_MODULE_ID_TYPE = List("sbt.ModuleID", "_root_.sbt.librarymanagement.ModuleID")
  val SEQ: String = "Seq"
  val SEQ_ADD_OPS = Set("+=", "++=")
  val SBT_LIB_CONFIGURATION = "_root_.sbt.librarymanagement.Configuration"
  val SBT_ORG_ARTIFACT = "_root_.sbt.librarymanagement.DependencyBuilders.OrganizationArtifactName"

  def sbtFilePattern: Capture[PsiElement] = psiElement.inFile {
    psiFile.withFileType(instanceOf(SbtFileType.getClass))
  }

  def infixExpressionChildPattern: Capture[PsiElement] = psiElement.withSuperParent(2, classOf[ScInfixExpr])
}
