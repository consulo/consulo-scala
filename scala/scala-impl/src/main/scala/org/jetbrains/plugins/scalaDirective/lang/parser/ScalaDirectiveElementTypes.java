package org.jetbrains.plugins.scalaDirective.lang.parser;

import consulo.language.version.LanguageVersion;
import consulo.language.version.LanguageVersionUtil;
import consulo.language.ast.ASTNode;
import consulo.language.Language;
import consulo.language.parser.ParserDefinition;
import consulo.language.parser.PsiBuilder;
import consulo.language.parser.PsiBuilderFactory;
import consulo.project.Project;
import com.intellij.java.language.psi.JavaPsiFacade;
import consulo.language.ast.ILazyParseableElementType;
import org.jspecify.annotations.Nullable;
import org.jetbrains.plugins.scalaDirective.psi.impl.ScDirectiveImpl;
import org.jetbrains.plugins.scalaDirective.ScalaDirectiveLanguage;

public interface ScalaDirectiveElementTypes {

    ILazyParseableElementType SCALA_DIRECTIVE = new ILazyParseableElementType("SCALA_DIRECTIVE", ScalaDirectiveLanguage.INSTANCE) {

        @Override
        @Nullable
        public ASTNode parseContents(ASTNode lazyNode) {
            Project project = JavaPsiFacade.getInstance(lazyNode.getTreeParent().getPsi().getProject()).getProject();

            Language language = getLanguage();
            LanguageVersion languageVersion = LanguageVersionUtil.findDefaultVersion(language);
            ParserDefinition parserDefinition = ParserDefinition.forLanguage(language);

            PsiBuilder builder = PsiBuilderFactory.getInstance()
                    .createBuilder(project, lazyNode, parserDefinition.createLexer(languageVersion), language, lazyNode.getText());

            ASTNode result = parserDefinition.createParser(languageVersion).parse(this, builder, languageVersion);

            return result.getFirstChildNode();
        }

        @Nullable
        @Override
        public ASTNode createNode(CharSequence text) {
            return text != null ? new ScDirectiveImpl(text, this) : null;
        }
    };
}
