package org.jetbrains.plugins.scala.lang.refactoring.introduceVariable;

import consulo.application.WriteAction;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.ProjectUndoManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import org.jetbrains.plugins.scala.ScalaBundle;
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScTypeAliasDefinition;
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScNamedElement;
import org.jetbrains.plugins.scala.lang.refactoring.rename.inplace.ScalaMemberInplaceRenamer;

import java.util.Objects;

/** Must use Java along with ScalaMemberInplaceRenamer to work around https://github.com/scala/scala3/issues/27208. */
public class ScalaInplaceTypeAliasIntroducer extends ScalaMemberInplaceRenamer {
    private final Editor editor;

    public ScalaInplaceTypeAliasIntroducer(ScNamedElement element, Editor editor) {
        super(element, element, editor, element.getName(), element.getName());
        this.editor = editor;
    }

    public static void revertState(Editor editor) {
        var project = editor.getProject();
        CommandProcessor.getInstance().executeCommand(project, () -> {
            var revertInfo = editor.getUserData(ScalaIntroduceVariableHandler.REVERT_INFO());
            var document = editor.getDocument();
            if (revertInfo != null) {
                WriteAction.run(() -> {
                    document.replaceString(0, document.getTextLength(), revertInfo.fileText());
                    PsiDocumentManager.getInstance(project).commitDocument(document);
                });
                editor.getCaretModel().moveToOffset(revertInfo.caretOffset());
                editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
                PsiDocumentManager.getInstance(editor.getProject()).commitDocument(document);
            }
            if (!project.isDisposed() && project.isOpen()) {
                PsiDocumentManager.getInstance(project).commitDocument(document);
            }
        }, ScalaBundle.message("command.introduce.type.alias"), null);
    }

    @Override
    public void setAdvertisementText(String text) {
        myAdvertisementText = ScalaBundle.message("press.hotkey.to.show.more.options");
    }

    @Override
    public boolean startsOnTheSameElement(RefactoringActionHandler handler, PsiElement element) {
        return element instanceof ScTypeAliasDefinition
                && Objects.equals(editor.getUserData(IntroduceTypeAlias.REVERT_TYPE_ALIAS_INFO()).getNamedElement(), element)
                && handler instanceof ScalaIntroduceVariableHandler;
    }

    @Override
    public void revertState() {
        // No state needs to be reverted for this introducer.
    }

    @Override
    protected void moveOffsetAfter(boolean success) {
        if (!success && myInsertedName != null && !ProjectUndoManager.getInstance(myProject).isUndoInProgress()
                && !editor.getUserData(IntroduceTypeAlias.REVERT_TYPE_ALIAS_INFO()).isCallModalDialogInProgress()) {
            var revertInfo = myEditor.getUserData(ScalaIntroduceVariableHandler.REVERT_INFO());
            if (revertInfo != null) {
                WriteAction.run(() -> {
                    PsiFile file = PsiDocumentManager.getInstance(myEditor.getProject()).getPsiFile(myEditor.getDocument());
                    myEditor.getDocument().replaceString(0, file.getTextLength(), revertInfo.fileText());
                });
                myEditor.getCaretModel().moveToOffset(revertInfo.caretOffset());
                myEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
            }
        }
    }
}
