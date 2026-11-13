package org.jetbrains.plugins.scala.lang.psi.impl

import com.intellij.java.language.impl.JavaFileType
import consulo.project.ProjectCoreUtil
import consulo.virtualFileSystem.VirtualFile
import consulo.language.impl.psi.LeafPsiElement
import consulo.language.ast.TokenSet
import consulo.language.psi.util.PsiTreeUtil
import consulo.language.psi.{PsiComment, PsiElement, PsiErrorElement}
import consulo.language.psi.event.PsiTreeChangeEvent
import consulo.language.ast.TokenType
import org.jetbrains.plugins.scala.api.ScalaFileType
import org.jetbrains.plugins.scala.extensions.ObjectExt
import org.jetbrains.plugins.scala.lang.lexer.{ScalaTokenType, ScalaTokenTypes}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports.ScImportExpr
import org.jetbrains.plugins.scala.lang.psi.impl.source.ScalaCodeFragment

private sealed trait ScalaPsiEventFilter {
  def shouldSkip(event: PsiTreeChangeEvent): Boolean = false

  def shouldSkip(changedElement: PsiElement): Boolean = false

  def shouldSkip(newElement: Option[PsiElement], oldElement: Option[PsiElement]): Boolean =
    newElement.forall(shouldSkip) && oldElement.forall(shouldSkip)
}

private object ScalaPsiEventFilter {
  val defaultFilters: Seq[ScalaPsiEventFilter] =
    Seq(InternalEventsFilter, NonSignificantParentsFilter, IgnoreLeafElementsFilter)

  private object InternalEventsFilter extends ScalaPsiEventFilter {
    override def shouldSkip(event: PsiTreeChangeEvent): Boolean =
      isGenericChange(event) || isFromIdeaInternalFile(event)

    private def isFromIdeaInternalFile(event: PsiTreeChangeEvent) = {
      val vFile = event.getFile match {
        case null => event.getOldValue.asOptionOf[VirtualFile]
        case file =>
          val fileType = file.getFileType
          if (fileType == ScalaFileType.INSTANCE || fileType == JavaFileType.INSTANCE) None
          else Option(file.getVirtualFile)
      }
      vFile.exists(ProjectCoreUtil.isProjectOrWorkspaceFile(_))
    }

    // upstream: PsiTreeChangeEventImpl.isGenericChange (internal in Consulo)
    private def isGenericChange(event: PsiTreeChangeEvent) = false
  }

  private object NonSignificantParentsFilter extends ScalaPsiEventFilter {
    override def shouldSkip(changedElement: PsiElement): Boolean = {
      // do not update on changes in dummy file or comments
      PsiTreeUtil.getParentOfType(changedElement, classOf[ScalaCodeFragment], classOf[PsiComment]) != null
    }
  }

  private object IgnoreLeafElementsFilter extends ScalaPsiEventFilter {

    import ScalaTokenTypes._
    import TokenType._

    private val significantTokens: TokenSet = {
      TokenSet.orSet(IDENTIFIER_TOKEN_SET, KEYWORDS, LITERALS, TokenSet.create(tUNDER))
    }

    //consider last dot and underscore in import expressions as significant
    private def isInImportExpression(validElement: PsiElement): Boolean =
      validElement.is[ScImportExpr] || validElement.getParent.is[ScImportExpr]

    override def shouldSkip(changedElement: PsiElement): Boolean = {
      changedElement match {
        case leaf: LeafPsiElement if isInImportExpression(leaf) => false
        case leaf: LeafPsiElement =>
          leaf.getElementType match {
            case WHITE_SPACE | BAD_CHARACTER => true
            case scalaToken: ScalaTokenType  => !significantTokens.contains(scalaToken)
            case _                           => false
          }
        case _: PsiErrorElement              => true
        case _                               => false
      }
    }
  }
}
