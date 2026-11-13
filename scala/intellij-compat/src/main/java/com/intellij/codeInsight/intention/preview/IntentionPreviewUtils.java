// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.codeInsight.intention.preview;

import consulo.language.editor.FileModificationService;
import consulo.language.editor.intention.IntentionAction;
import consulo.application.WriteAction;
import consulo.codeEditor.Editor;
import consulo.util.dataholder.Key;
import consulo.application.util.function.ThrowableComputable;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.util.lang.function.ThrowableRunnable;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

/**
 * Utils to support intention preview feature
 */
public final class IntentionPreviewUtils {
  private static final Key<PsiFile> PREVIEW_MARKER = Key.create("PREVIEW_MARKER");
  private static final ThreadLocal<Editor> PREVIEW_EDITOR = new ThreadLocal<>();

  /**
   * This method is a part of the internal implementation of an intention preview mechanism.
   * It's not intended to be called in the client code.
   *
   * @param file file to get the preview copy
   * @return a preview copy of the file
   */
  // upstream: @ApiStatus.Internal
  public static PsiFile obtainCopyForPreview(PsiFile file) {
    return obtainCopyForPreview(file, file);
  }

  /**
   * This method is a part of the internal implementation of an intention preview mechanism.
   * It's not intended to be called in the client code.
   *
   * @param file         file to get the preview copy
   * @param originalFile original file. May differ from file if we are inside the injection
   * @return a preview copy of the file
   */
  // upstream: @ApiStatus.Internal
  public static PsiFile obtainCopyForPreview(PsiFile file, PsiFile originalFile) {
    PsiFile copy = (PsiFile)file.copy();
    markAsPreviewCopy(copy, originalFile);
    return copy;
  }

  // upstream: @ApiStatus.Internal
  public static void markAsPreviewCopy(PsiFile copy, PsiFile originalFile) {
    copy.putUserData(PREVIEW_MARKER, originalFile);
  }

  /**
   * @param element element to check
   * @return true if a given element is a copy created for preview
   */
  public static boolean isPreviewElement(PsiElement element) {
    PsiFile file = element.getContainingFile();
    return file != null && file.getUserData(PREVIEW_MARKER) != null;
  }

  /**
   * @param previewFile file from preview session (e.g., passed to {@link IntentionAction#generatePreview})
   * @return original physical file, or null if this file is not the preview copy
   */
  public static @Nullable PsiFile getOriginalFile(PsiFile previewFile) {
    return previewFile.getUserData(PREVIEW_MARKER);
  }

  /**
   * Prepares an element for writing, taking into account that it could be a preview element
   *
   * @param element element to prepare
   * @return true if an element can be written
   */
  public static boolean prepareElementForWrite(PsiElement element) {
    if (isPreviewElement(element)) return true;
    return FileModificationService.getInstance().preparePsiElementForWrite(element);
  }

  /**
   * Performs a preview-aware write-action. Execute action immediately if preview is active; wrap into write action otherwise.
   *
   * @param action action to execute
   * @param <E>    exception the action throws
   * @throws E if action throws
   */
  public static <E extends Throwable> void write(ThrowableRunnable<E> action) throws E {
    if (PREVIEW_EDITOR.get() != null) {
      action.run();
    }
    else {
      WriteAction.run(action);
    }
  }

  /**
   * Performs a preview-aware write-action. Execute action immediately if preview is active; wrap into write action otherwise.
   *
   * @param action action to execute
   * @param <E>    exception the action throws
   * @return result of the action
   * @throws E if action throws
   */
  public static <T, E extends Throwable> T writeAndCompute(ThrowableComputable<T, E> action) throws E {
    if (PREVIEW_EDITOR.get() != null) {
      return action.compute();
    }
    else {
      return WriteAction.compute(action);
    }
  }

  /**
   * Start a preview session with given editor (generatePreview call should be wrapped)
   *
   * @param editor   preview editor to use
   * @param runnable action to execute
   */
  // upstream: @ApiStatus.Internal
  public static void previewSession(Editor editor, Runnable runnable) {
    PREVIEW_EDITOR.set(editor);
    try {
      runnable.run();
    }
    finally {
      PREVIEW_EDITOR.remove();
    }
  }

  /**
   * @return current imaginary editor used for preview; null if we are not in preview session
   */
  @Contract(pure = true)
  public static @Nullable Editor getPreviewEditor() {
    return PREVIEW_EDITOR.get();
  }

  /**
   * @return true if intention preview is currently being computed in this thread
   */
  @Contract(pure = true)
  public static boolean isIntentionPreviewActive() {
    return PREVIEW_EDITOR.get() != null;
  }

  // upstream: getModCommandPreview(ModCommand, ActionContext) - no ModCommand API in Consulo

  /**
   * Tries to convert an element from an original file to the same element in the preview file.
   * This method should be used only before any modification to the preview file is made.
   * <p>
   * If {@code file} is an original file or {@code element} is already a preview element, then {@code element} is returned as is.
   * Otherwise an attempt to convert the element to a preview element is made. For that to succeed, {@code element} must be from
   * the original file of the preview {@code file}.
   *
   */
  public static <T extends PsiElement> @Nullable T toPreviewElementIfPreviewFile(PsiFile file, T element) {
    if (!isPreviewElement(file) || isPreviewElement(element)) return element;
    if (getOriginalFile(file) != element.getContainingFile()) return null;
    var range = element.getTextRange();
    //noinspection unchecked
    return (T)PsiTreeUtil.findElementOfClassAtRange(file, range.getStartOffset(), range.getEndOffset(),
                                                 element.getClass());
  }
}
