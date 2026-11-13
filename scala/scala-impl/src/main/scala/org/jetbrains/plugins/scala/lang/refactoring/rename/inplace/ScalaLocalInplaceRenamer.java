package org.jetbrains.plugins.scala.lang.refactoring.rename.inplace;

import consulo.language.Language;
import consulo.codeEditor.Editor;
import consulo.document.RangeMarker;
import consulo.project.Project;
import consulo.util.lang.Pair;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiReference;
import consulo.content.scope.SearchScope;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.rename.inplace.VariableInplaceRenamer;
import org.jetbrains.plugins.scala.lang.refactoring.ScalaNamesValidator$;
import org.jetbrains.plugins.scala.lang.refactoring.rename.ScalaRenameUtil;
import org.jetbrains.plugins.scala.lang.refactoring.util.ScalaNamesUtil;

import java.util.Collection;
import java.util.List;

import static consulo.language.editor.refactoring.util.TextOccurrencesUtil.processUsagesInStringsAndComments;

/**
 * Implement this inheritance in Java until https://github.com/scala/scala3/issues/27208 is fixed in Scala 3.9.
 * Scala's incremental API extraction crashes on VariableInplaceRenamer's private sealed interface.
 */
public class ScalaLocalInplaceRenamer extends VariableInplaceRenamer {
    private final PsiNamedElement elementToRename;
    private final RangeMarker elementRange;

    public ScalaLocalInplaceRenamer(PsiNamedElement elementToRename, Editor editor, Project project,
                                   String initialName, String oldName) {
        super(elementToRename, editor, project, initialName, oldName);
        this.elementToRename = elementToRename;
        elementRange = editor.getDocument().createRangeMarker(elementToRename.getTextRange());
    }

    public ScalaLocalInplaceRenamer(PsiNamedElement elementToRename, Editor editor) {
        this(elementToRename, editor, elementToRename.getProject(),
                ScalaNamesUtil.scalaName(elementToRename), ScalaNamesUtil.scalaName(elementToRename));
    }

    @Override
    public void collectAdditionalElementsToRename(List<Pair<PsiElement, TextRange>> stringUsages) {
        String stringToSearch = ScalaNamesUtil.scalaName(elementToRename);
        if (stringToSearch == null) return;

        PsiFile file = currentFile();
        if (file == null) return;
        // upstream: searched in LocalSearchScope(file); Consulo's TextOccurrencesUtil has no scope overload,
        // so results are filtered by the current file instead
        processUsagesInStringsAndComments(elementToRename, stringToSearch, true,
                (psiElement, textRange) -> {
                    if (psiElement.getContainingFile() == file) {
                        stringUsages.add(Pair.create(psiElement, textRange));
                    }
                    return true;
                });
    }

    @Override
    public Collection<PsiReference> collectRefs(SearchScope referencesSearchScope) {
        Collection<PsiReference> references = super.collectRefs(referencesSearchScope);
        ScalaRenameUtil.addEndMarkerReference(getVariable(), references);
        return references;
    }

    @Override
    public boolean isIdentifier(String newName, Language language) {
        return ScalaNamesValidator$.MODULE$.isIdentifier(newName);
    }

    @Override
    public boolean startsOnTheSameElement(RefactoringActionHandler handler, PsiElement element) {
        return handler instanceof ScalaLocalInplaceRenameHandler && ScalaRenameUtil.sameElement(elementRange, element);
    }

    @Override
    public PsiElement checkLocalScope() {
        PsiFile file = currentFile();
        return file == null ? super.checkLocalScope() : file;
    }

    private PsiFile currentFile() {
        return PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
    }
}
