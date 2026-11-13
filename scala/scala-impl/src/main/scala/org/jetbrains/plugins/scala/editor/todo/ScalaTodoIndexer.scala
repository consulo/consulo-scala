package org.jetbrains.plugins.scala.editor.todo

import consulo.language.psi.stub.todo.LexerBasedTodoIndexer
import consulo.language.psi.stub.{BaseFilterLexer, OccurrenceConsumer}
import consulo.language.psi.search.UsageSearchContext.IN_COMMENTS
import consulo.virtualFileSystem.fileType.FileType
import org.jetbrains.plugins.scala.api.ScalaFileType
import org.jetbrains.plugins.scala.lang.lexer.{ScalaLexer, ScalaTokenTypes}

// upstream: also registered for the sbt and Scala Worksheet file types, Consulo needs an indexer class per file type
class ScalaTodoIndexer extends LexerBasedTodoIndexer {

  override def getFileType: FileType = ScalaFileType.INSTANCE

  override def createLexer(consumer: OccurrenceConsumer): BaseFilterLexer =
    new ScalaTodoIndexer.ScalaFilterLexer(consumer)
}

object ScalaTodoIndexer {

  /**
   * This basic implementation was only added to support proper TO-DO indices for ScalaDoc
   *
   * see [[idCache.JavaFilterLexer]]
   */
  private final class ScalaFilterLexer(consumer: OccurrenceConsumer)
    extends BaseFilterLexer(new ScalaLexer(false, null), consumer) {

    override def advance(): Unit = {
      val tokenType = getDelegate.getTokenType

      if (ScalaTokenTypes.COMMENTS_TOKEN_SET.contains(tokenType)) {
        scanWordsInToken(IN_COMMENTS, false, false)
        advanceTodoItemCountsInToken()
      }

      getDelegate.advance()
    }
  }
}