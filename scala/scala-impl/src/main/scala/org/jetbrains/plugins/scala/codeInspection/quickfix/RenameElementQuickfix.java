package org.jetbrains.plugins.scala.codeInspection.quickfix;

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils;
import consulo.language.editor.inspection.LocalQuickFixOnPsiElement;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.refactoring.RefactoringQuickFix;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.application.util.HtmlChunk;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.action.RefactoringActionHandlerFactory;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.usage.UsageViewUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScNamedElement;

public class RenameElementQuickfix extends LocalQuickFixOnPsiElement implements RefactoringQuickFix {
    private final @Nls String name;

    public RenameElementQuickfix(PsiElement myRef, @Nls String name) {
        super(myRef);
        this.name = name;
    }

    @Override
    public LocalizeValue getText() {
        return LocalizeValue.of(name);
    }

    @Override
    public void invoke(Project project, PsiFile file, PsiElement startElement, PsiElement endElement) {
        if (!IntentionPreviewUtils.prepareElementForWrite(file) || !startElement.isValid()) return;
        doFix(startElement);
    }

    @Override
    public RefactoringActionHandler getHandler() {
        return RefactoringActionHandlerFactory.getInstance().createRenameHandler();
    }

    // upstream: getHandler(DataContext) picked a context specific rename handler; Consulo's RefactoringQuickFix
    // has no such hook - the rename handler created by RefactoringActionHandlerFactory dispatches by data context itself

    // upstream: override of LocalQuickFix.generatePreview (no intention preview in Consulo)
    public IntentionPreviewInfo generatePreview(Project project, ProblemDescriptor previewDescriptor) {
        PsiElement element = getElementToRefactor(previewDescriptor.getPsiElement());
        if (element instanceof ScNamedElement namedElement) {
            String what = UsageViewUtil.getType(element) + " '" + namedElement.name() + "'";
            String message = RefactoringBundle.message("rename.0.and.its.usages.preview.text", what);
            return new IntentionPreviewInfo.Html(HtmlChunk.text(message));
        }
        return IntentionPreviewInfo.EMPTY;
    }
}
