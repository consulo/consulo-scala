package org.jetbrains.plugins.scala.api.lang.psi.stubs;

import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.StubElement;

/**
 * Redeclares the Java default method as abstract so Scala stub traits use the final
 * implementation inherited from {@link com.intellij.psi.stubs.StubBase}.
 * This declaration must be in Java: an abstract Scala override does not suppress
 * an inherited concrete default method. It lives in scala-api so javac compiles
 * it before scalac reads it; mixed Java/Scala compilation does not preserve this
 * distinction either. Scala subtraits must also inherit this interface directly
 * to retain the abstract declaration when resolving inherited members.
 */
public interface ScStubElement<T extends PsiElement> extends StubElement<T> {
    // upstream overrides StubElement#findChildStubByElementType(IElementType) to mark result @Nullable.
    // Consulo has no such method - StubElement#findChildStubByType(IStubElementType) is already @Nullable
}
