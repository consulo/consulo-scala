package org.jetbrains.plugins.scala.copyright

import consulo.annotation.component.ExtensionImpl
import consulo.language.copyright.config.CopyrightProfile
import consulo.language.copyright.{BaseUpdateCopyrightsProvider, UpdatePsiFileCopyright}
import consulo.language.copyright.config.CopyrightFileConfig
import consulo.language.psi.{PsiComment, PsiFile, PsiWhiteSpace, SyntaxTraverser}
import consulo.util.collection.TreeTraversal
import consulo.virtualFileSystem.fileType.FileType
import org.jetbrains.plugins.scala.api.ScalaFileType
import org.jetbrains.plugins.scala.extensions.{&, ObjectExt, Parent, PsiElementExt}
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScCommentOwner
import org.jetbrains.plugins.scalaDirective.psi.api.{ScDirective, ScDirectiveToken}

import scala.jdk.CollectionConverters.{IterableHasAsScala, SeqHasAsJava}

@ExtensionImpl
final class UpdateScalaCopyrightsProvider extends BaseUpdateCopyrightsProvider {

  override def getFileType: FileType = ScalaFileType.INSTANCE

  override def createInstance(file: PsiFile, options: CopyrightProfile): UpdatePsiFileCopyright[CopyrightFileConfig] =
    new UpdateScalaCopyright(file, options)
}

private final class UpdateScalaCopyright(
  file: PsiFile,
  options: CopyrightProfile
) extends UpdatePsiFileCopyright[CopyrightFileConfig](file, options) {
  override def accept(): Boolean = getFile.is[ScalaFile]

  override def scanFile(): Unit = {
    val file = getFile
    val comments = UpdateScalaCopyright.getExistingComments(file)
    checkComments(comments.lastOption.orNull, true, comments.asJava)
  }
}

private object UpdateScalaCopyright {
  private def getExistingComments(file: PsiFile): List[PsiComment] = SyntaxTraverser.psiTraverser(file)
    .withTraversal(TreeTraversal.LEAVES_DFS)
    .traverse()
    .takeWhile { e =>
      e.is[PsiWhiteSpace, ScDirectiveToken] ||
        e.is[PsiComment] && !e.parent.exists(_.isInstanceOf[ScCommentOwner]) ||
        e.getText.isEmpty
    }
    .asScala
    .toList
    .collect {
      case comment: PsiComment => comment
      case (_: ScDirectiveToken) & Parent(directive: ScDirective) => directive
    }
    .distinct
}
