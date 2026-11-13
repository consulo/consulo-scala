package org.jetbrains.plugins.scala.lang.findUsages;

import consulo.language.cacheBuilder.DefaultWordsScanner;
import consulo.language.cacheBuilder.WordOccurrence;
import consulo.language.lexer.Lexer;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import java.util.function.Predicate;
import org.jetbrains.plugins.scala.lang.lexer.ScalaLexer;
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes;

public class ScalaWordsScanner extends DefaultWordsScanner {
  private final Lexer myLexer;
  private final TokenSet myIdentifierTokenSet;
  private final TokenSet myCommentTokenSet;
  private final TokenSet myLiteralTokenSet;
  private final TokenSet mySkipCodeContextTokenSet;

  public ScalaWordsScanner() {
    this(new ScalaLexer(false, null), ScalaTokenTypes.IDENTIFIER_TOKEN_SET, ScalaTokenTypes.COMMENTS_TOKEN_SET, ScalaTokenTypes.STRING_LITERAL_TOKEN_SET, TokenSet.EMPTY);
  }

  public ScalaWordsScanner(final Lexer lexer, final TokenSet identifierTokenSet, final TokenSet commentTokenSet,
                             final TokenSet literalTokenSet, TokenSet skipCodeContextTokenSet) {
    super(lexer, identifierTokenSet, commentTokenSet, literalTokenSet, skipCodeContextTokenSet);
    myLexer = lexer;
    myIdentifierTokenSet = identifierTokenSet;
    myCommentTokenSet = commentTokenSet;
    myLiteralTokenSet = literalTokenSet;
    mySkipCodeContextTokenSet = skipCodeContextTokenSet;
  }

  @Override
  public void processWords(CharSequence fileText,
                           Predicate<WordOccurrence> processor) {
    myLexer.start(fileText);
    WordOccurrence occurrence = new WordOccurrence(fileText, 0, 0, null); // shared occurrence

    IElementType type;
    while ((type = myLexer.getTokenType()) != null) {
      if (myIdentifierTokenSet.contains(type)) {
        occurrence.init(fileText, myLexer.getTokenStart(), myLexer.getTokenEnd(), WordOccurrence.Kind.CODE);
        if (!processor.test(occurrence)) return;
        if (!stripWords(processor, fileText, myLexer.getTokenStart(), myLexer.getTokenEnd(), WordOccurrence.Kind.CODE, occurrence, false)) return;      }
      else if (myCommentTokenSet.contains(type)) {
        if (!stripWords(processor, fileText,myLexer.getTokenStart(),myLexer.getTokenEnd(), WordOccurrence.Kind.COMMENTS,occurrence, false)) return;
      }
      else if (myLiteralTokenSet.contains(type)) {
        boolean mayHaveFileRefs = true; // for indexing string literals as references to property keys and files
        if (!stripWords(processor, fileText, myLexer.getTokenStart(),myLexer.getTokenEnd(),WordOccurrence.Kind.LITERALS,occurrence, mayHaveFileRefs)) return;
      }
      else if (!mySkipCodeContextTokenSet.contains(type)) {
        if (!stripWords(processor, fileText, myLexer.getTokenStart(), myLexer.getTokenEnd(), WordOccurrence.Kind.CODE, occurrence, false)) return;
      }
      myLexer.advance();
    }
  }

  // upstream: WordsScanner.getVersion (no such method in Consulo)
  public int getVersion() {
    return 3;
  }
}
