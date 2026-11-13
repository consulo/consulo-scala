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

import consulo.virtualFileSystem.fileType.FileType;
import consulo.application.util.HtmlBuilder;
import consulo.application.util.HtmlChunk;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFile;
import consulo.util.collection.ContainerUtil;
import org.jetbrains.annotations.Nls;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Possible result for IntentionPreview.
 * @see com.intellij.codeInsight.intention.IntentionAction#generatePreview(Project, Editor, PsiFile)
 * @see com.intellij.codeInspection.LocalQuickFix#generatePreview(Project, ProblemDescriptor)
 */
// upstream: @ApiStatus.NonExtendable
public interface IntentionPreviewInfo {
  /**
   * No intention preview is available.
   */
  IntentionPreviewInfo EMPTY = new IntentionPreviewInfo() {
    @Override
    public String toString() {
      return "EMPTY";
    }
  };

  /**
   * Try to use fallback mechanism for intention preview instead.
   * Do not use this directly
   */
  // upstream: @ApiStatus.Internal
  IntentionPreviewInfo FALLBACK_DIFF = new IntentionPreviewInfo() {
    @Override
    public String toString() {
      return "FALLBACK";
    }
  };

  /**
   * Changes in the file copy should be displayed as intention preview. Differences in leading/trailing 
   * whitespaces will be shown only if there are no other differences. Otherwise, whitespaces will be trimmed before shown the diff.
   */
  IntentionPreviewInfo DIFF = new IntentionPreviewInfo() {
    @Override
    public String toString() {
      return "DIFF";
    }
  };

  /**
   * Changes in the file copy should be displayed as intention preview, without trimming any whitespaces.
   */
  IntentionPreviewInfo DIFF_NO_TRIM = new IntentionPreviewInfo() {
    @Override
    public String toString() {
      return "DIFF_NO_TRIM";
    }
  };

  /**
   * Diff preview for multiple files. UI may show only some of them if there are too many.
   */
  // upstream: MultiFileDiff, ModernDiff, Snippet, navigate, movePsi and other members are not ported yet (no preview UI in Consulo)

  final class CustomDiff implements IntentionPreviewInfo {
    private final FileType myFileType;
    private final String myOrigText;
    private final String myModifiedText;
    private final @Nullable String myFileName;
    private final boolean myLineNumbers;

    /**
     * Construct a custom diff. Please prefer another constructor and specify a file name if it's applicable.
     *
     * @param type         file type, used for highlighting
     * @param origText     original file text
     * @param modifiedText changed file text
     */
    public CustomDiff(FileType type, String origText, String modifiedText) {
      this(type, null, origText, modifiedText);
    }

    /**
     * @param type         file type, used for highlighting
     * @param name         file name, can be displayed to user if specified
     * @param origText     original file text
     * @param modifiedText changed file text
     */
    public CustomDiff(FileType type, @Nullable String name, String origText, String modifiedText) {
      this(type, name, origText, modifiedText, false);
    }

    /**
     * @param type         file type, used for highlighting
     * @param name         file name, can be displayed to user if specified
     * @param origText     original file text
     * @param modifiedText changed file text
     * @param lineNumbers  if true then diff will display line numbers
     */
    public CustomDiff(FileType type,
                      @Nullable String name,
                      String origText,
                      String modifiedText,
                      boolean lineNumbers) {
      myFileType = type;
      myFileName = name;
      myOrigText = origText;
      myModifiedText = modifiedText;
      myLineNumbers = lineNumbers;
    }

    public boolean showLineNumbers() {
      return myLineNumbers;
    }

    public @Nullable String fileName() {
      return myFileName;
    }

    public FileType fileType() {
      return myFileType;
    }

    public String originalText() {
      return myOrigText;
    }

    public String modifiedText() {
      return myModifiedText;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      CustomDiff diff = (CustomDiff)o;
      return myLineNumbers == diff.myLineNumbers &&
             myFileType.equals(diff.myFileType) &&
             myOrigText.equals(diff.myOrigText) &&
             myModifiedText.equals(diff.myModifiedText) &&
             Objects.equals(myFileName, diff.myFileName);
    }

    @Override
    public int hashCode() {
      int result = myFileType.hashCode();
      result = 31 * result + myOrigText.hashCode();
      result = 31 * result + myModifiedText.hashCode();
      result = 31 * result + Objects.hashCode(myFileName);
      result = 31 * result + Boolean.hashCode(myLineNumbers);
      return result;
    }

    @Override
    public String toString() {
      return "CustomDiff{" +
             "myFileType=" + myFileType +
             ", myOrigText='" + myOrigText + '\'' +
             ", myModifiedText='" + myModifiedText + '\'' +
             ", myFileName='" + myFileName + '\'' +
             ", myLineNumbers=" + myLineNumbers +
             '}';
    }
  }

  /**
   * Kind of information displayed in HTML preview
   */
  enum InfoKind {
    /**
     * Informational preview (default)
     */
    INFORMATION,
    /**
     * Error
     */
    ERROR
  }

  /**
   * HTML description. Here are some advices:
   * <ul>
   *   <li>If you want to display icon, use {@link HtmlChunk#icon(String, Icon)}. Though be careful, as converting it to text
   *   and back (e.g., via {@link HtmlChunk#raw(String)}) would make the icon invalid. In general, avoid using {@link HtmlChunk#raw(String)}.
   *   Consider using {@link HtmlChunk#template(String, Map.Entry[])} if necessary</li>
   *   <li>Note that the description pane has fixed width (300 px for default font, see
   *   {@link com.intellij.codeInsight.intention.impl.preview.IntentionPreviewPopupUpdateProcessor#MIN_WIDTH IntentionPreviewPopupUpdateProcessor#MIN_WIDTH}).
   *   Try to make the description not very long to avoid vertical scrollbar, as it's hardly possible to scroll it.</li>
   *   <li>In general, any user interaction with the description pane is hardly possible, as if you move the mouse it’s likely
   *   that preview for another item will be displayed. Also, preview is mostly useful for keyboard users, as it’s usually
   *   displayed as a reaction on Alt+Enter. So adding clickable links or buttons there is not a good idea.</li>
   *   <li>When possible, use generic preview methods available in {@link IntentionPreviewInfo} (e.g., {@link #rename(PsiFile, String)},
   *   {@link #navigate(NavigatablePsiElement)}, {@link #movePsi(PsiNamedElement, PsiNamedElement)}, {@link #moveMultiplePsi(List, PsiNamedElement)}). They provide uniform
   *   preview for common cases. Ask if you think that you need a new common method.</li>
   * </ul>
   */
  final class Html implements IntentionPreviewInfo {
    private final HtmlChunk myContent;
    private final InfoKind myInfoKind;

    /**
     * Construct description from HtmlChunk
     *
     * @param content description content
     */
    public Html(HtmlChunk content) {
      this(content, InfoKind.INFORMATION);
    }

    /**
     * Construct description from HtmlChunk
     *
     * @param content  description content
     * @param infoKind kind of description box (may affect visual representation)
     */
    public Html(HtmlChunk content, InfoKind infoKind) {
      myContent = content;
      myInfoKind = infoKind;
    }

    /**
     * Construct description from raw HTML string
     *
     * @param contentHtml raw HTML content
     */
    public Html(@Nls String contentHtml) {
      this(HtmlChunk.raw(contentHtml));
    }

    /**
     * @return HTML content
     */
    public HtmlChunk content() {
      return myContent;
    }

    public InfoKind infoKind() {
      return myInfoKind;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Html html = (Html)o;
      return myContent.toString().equals(html.myContent.toString()) && myInfoKind == html.myInfoKind;
    }

    @Override
    public int hashCode() {
      return 31 * myContent.toString().hashCode() + myInfoKind.hashCode();
    }

    @Override
    public String toString() {
      return "HTML [content=" + myContent + ", infoKind=" + myInfoKind + "]";
    }
  }

  /**
   * @param file    file to be renamed
   * @param newName new file name (with extension)
   * @return a preview that visualizes the file rename
   */
  static IntentionPreviewInfo rename(PsiFile file, String newName) {
    return rename(Objects.requireNonNull(file.getVirtualFile()), newName);
  }

  /**
   * @param file    file to be renamed
   * @param newName new file name (with extension)
   * @return a preview that visualizes the file rename
   */
  static IntentionPreviewInfo rename(VirtualFile file, String newName) {
    // upstream: file icon chunk before both names (IconUtil.getIcon + HtmlChunk.icon) - Consulo HtmlChunk has no icon chunk
    HtmlChunk fragment = new HtmlBuilder()
      .append(file.getName())
      .append(" ").append(HtmlChunk.htmlEntity("&rarr;")).append(" ")
      .append(newName)
      .toFragment();
    return new Html(fragment.wrapWith("p"));
  }

  /**
   * @param updatedList list after updating (containing new option)
   * @param addedOption an option added to the list
   * @param title a title text for the list
   * @return a presentation describing that the action will add the specified option to the options list
   */
  static IntentionPreviewInfo addListOption(List<String> updatedList,
                                            String addedOption,
                                            @Nls String title) {
    return addListOption(updatedList, title, Predicate.isEqual(addedOption));
  }

  /**
   * @param updatedList list after updating (containing new option)
   * @param title       a title text for the list
   * @param toSelect    predicate, which returns true if the option should be selected in preview
   * @return a presentation describing that the action will add the specified option to the options list
   */
  static IntentionPreviewInfo.Html addListOption(List<String> updatedList,
                                            @Nls String title,
                                            Predicate<String> toSelect) {
    int maxToList = Math.min(7, updatedList.size() + 2);
    HtmlChunk select = HtmlChunk.tag("select").attr("multiple", "multiple").attr("size", maxToList)
      .children(ContainerUtil.map2Array(updatedList, HtmlChunk.class, pref -> {
        HtmlChunk.Element chunk = HtmlChunk.tag("option").addText(pref);
        return toSelect.test(pref) ? chunk.attr("selected", "selected") : chunk;
      }));
    HtmlChunk content = new HtmlBuilder().append(title)
      .br().br()
      .append(select)
      .toFragment();
    return new Html(content);
  }
}
