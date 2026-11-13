package org.jetbrains.plugins.scala.lang.scaladoc.parser.parsing.markdown;

import consulo.language.ast.IElementType;
import org.intellij.markdown.MarkdownElementTypes;
import org.intellij.markdown.MarkdownTokenTypes;
import org.intellij.markdown.parser.LookaheadText;
import org.intellij.markdown.parser.ProductionHolder;
import org.intellij.markdown.parser.constraints.MarkdownConstraints;
import org.intellij.markdown.parser.markerblocks.MarkerBlockImpl;
import org.intellij.markdown.parser.sequentialparsers.SequentialParser;

import java.util.Collections;
import java.util.regex.Pattern;

/**
 * A copy of {@link org.intellij.markdown.parser.markerblocks.impl.CodeFenceMarkerBlock} whose closing fence is a regex.
 * <p>
 * upstream: used CodeFenceMarkerBlock directly, whose Kotlin version put the fence into its end line regex unescaped,
 * which {@link ScalaDocCodeFenceProvider} relies on for the ScalaDoc closing fence. The Consulo engine quotes the fence
 * (and the class is final), so the block is copied here with the unquoted fence.
 */
public final class ScalaDocCodeFenceMarkerBlock extends MarkerBlockImpl {
    private final ProductionHolder productionHolder;
    private final Pattern endLinePattern;
    private int realInterestingOffset = -1;

    public ScalaDocCodeFenceMarkerBlock(MarkdownConstraints constraints, ProductionHolder productionHolder, String fenceEndRegex) {
        super(constraints, productionHolder.mark());
        this.productionHolder = productionHolder;
        this.endLinePattern = Pattern.compile("^ {0,3}" + fenceEndRegex + "+ *$");
    }

    @Override
    public boolean allowsSubBlocks() {
        return false;
    }

    @Override
    public boolean isInterestingOffset(LookaheadText.Position position) {
        return true;
    }

    @Override
    protected int calcNextInterestingOffset(LookaheadText.Position position) {
        return position.getNextLineOrEofOffset();
    }

    @Override
    protected ClosingAction getDefaultAction() {
        return ClosingAction.DONE;
    }

    @Override
    protected ProcessingResult doProcessToken(LookaheadText.Position position, MarkdownConstraints currentConstraints) {
        if (position.getOffset() < realInterestingOffset) {
            return ProcessingResult.CANCEL;
        }

        // Eat everything if we're on code line
        if (position.getOffsetInCurrentLine() != -1) {
            return ProcessingResult.CANCEL;
        }

        MarkdownConstraints nextLineConstraints = MarkdownConstraints.applyToNextLineAndAddModifiers(myConstraints, position);
        if (!MarkdownConstraints.extendsPrev(nextLineConstraints, myConstraints)) {
            return ProcessingResult.DEFAULT;
        }

        int nextLineOffset = position.getNextLineOrEofOffset();
        realInterestingOffset = nextLineOffset;

        CharSequence currentLine = MarkdownConstraints.eatItselfFromString(nextLineConstraints, position.getCurrentLine());
        if (endLinePattern.matcher(currentLine).matches()) {
            productionHolder.addProduction(Collections.singletonList(new SequentialParser.Node(
                position.getOffset() + 1, position.getNextLineOrEofOffset(), MarkdownTokenTypes.CODE_FENCE_END)));
            scheduleProcessingResult(nextLineOffset, ProcessingResult.DEFAULT);
        }
        else {
            int contentStart = Math.min(position.getOffset() + 1 + MarkdownConstraints.getCharsEaten(myConstraints, position.getCurrentLine()), nextLineOffset);
            if (contentStart < nextLineOffset) {
                productionHolder.addProduction(Collections.singletonList(new SequentialParser.Node(
                    contentStart, nextLineOffset, MarkdownTokenTypes.CODE_FENCE_CONTENT)));
            }
        }

        return ProcessingResult.CANCEL;
    }

    @Override
    public IElementType getDefaultNodeType() {
        return MarkdownElementTypes.CODE_FENCE;
    }
}
